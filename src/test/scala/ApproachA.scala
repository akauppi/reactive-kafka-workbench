package test

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializerSettings, SourceShape, ActorMaterializer}
import akka.stream.scaladsl.{Flow, GraphDSL, Sink, Source}
import com.softwaremill.react.kafka.{ReactiveKafka, ConsumerProperties}
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.scalatest.{FlatSpec, Matchers}
import org.apache.kafka.common.serialization._
import com.softwaremill.react.kafka2._
import java.util.UUID

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Await
import scala.concurrent.duration._

/*
* Based on the New API
*   -> https://github.com/softwaremill/reactive-kafka/blob/master/docs/NewAPI.md
*   -> https://github.com/softwaremill/reactive-kafka/blob/master/core/src/main/scala/com/softwaremill/react/kafka2/DummyProducer.scala
*   -> https://github.com/softwaremill/reactive-kafka/blob/master/core/src/main/scala/com/softwaremill/react/kafka2/DummyConsumer.scala
*
* Ref.
*   Kafka Producer properties -> http://kafka.apache.org/documentation.html#producerconfigs
*/
class ApproachATest extends FlatSpec with Matchers {

  val topic = UUID.randomUUID.toString   // e.g. "d8a630f0-093e-4022-b6f4-8fb1d8215f58"

  info(s"Using topic: $topic")

  val data: Seq[Int] = testData

  //---
  behavior of "Approach A"

  it should "Be able to write to Kafka" in {

    implicit val actorSystem = ActorSystem("ApproachA_write")
    implicit val materializer = ActorMaterializer(
      ActorMaterializerSettings(actorSystem)
        //.withAutoFusing(true)       // Akka Streams: fusing multiple stages together (on same actor) or not (no functional difference!)
        //.withInputBuffer(1024,1024)   // Akka Streams: input buffer size (only first value currently (2.4.2) matters; is the initial size)
    )

    val keySerializer = new ByteArraySerializer // Note: no idea why this, reactive-kafka 'DummyProducer' sample uses it
    val valSerializer = new IntegerSerializer

    val producerSink = Producer.sink(
      ProducerProvider(host, keySerializer, valSerializer)
        .props(
          "acks" -> "1"                  // let Kafka server acknowledge receiving each entry
        )
    )

    var count=0

    Source
      .fromIterator(() => data.toIterator)
      .map(new java.lang.Integer(_))
      .via(Producer.value2record(topic))      // converts to 'ProducerRecord[Array[Byte], V]' (note: if using keys, replace this with our own, explicit mapping)
      //.via(producer)                          // Q: this the place that actually writes to Kafka?
      //.mapAsync(1)(identity)                  // Q: what does this do? (it's an Akka Streams thing)
      //.to(shutdownAsOnComplete)               // clean up the actor system, when stream is ready
      .map( x => { println(x.value); count += 1; x } )
      .to(producerSink)
      .run()

    // Note: In the above, only the first integer gets processed. Why?? AKa250216

    Thread.sleep(5000)

    // tbd. Should we wait here - are the things now in Kafka?

    count should be (data.length)
  }

  it should "Be able to read from Kafka" ignore /*in*/ {

    implicit val actorSystem = ActorSystem("ApproachA_read")
    implicit val materializer = ActorMaterializer(
      ActorMaterializerSettings(actorSystem)
          //.withAutoFusing(false)
          .withInputBuffer(16,16)
    )

    val keyDeserializer = new ByteArrayDeserializer
    val valDeserializer = new IntegerDeserializer

    var received = new ArrayBuffer[Int]

    val kafka = new ReactiveKafka

    val consumerProps = ConsumerProperties(
      bootstrapServers = "localhost:9092",    // NOT 'brokerList' like reactive-kafka README says
      topic = topic,
      groupId = consumerGroupId,
      valueDeserializer = new StringDeserializer
    )
      .commitInterval(5 seconds)

    val consumerWithOffsetSink = kafka.consumeWithOffsetSink(consumerProps)

    Source.fromPublisher(consumerWithOffsetSink.publisher)
      .map( x => {
        val v= x.value.toInt; println(v); received += v; x
      })
      .to(consumerWithOffsetSink.offsetCommitSink) // stream back for commit
      .run()

    /***
      * val prov = ConsumerProvider(host, keyDeserializer, valDeserializer)
      * .setup(TopicSubscription(topic))
      * .groupId(consumerGroupId)
      * .autoCommit(false)
      * .props("auto.offset.reset" -> "earliest")   // tbd. what's the purpose of this?

      * val graph = GraphDSL.create(Consumer[Array[Byte], java.lang.Integer](prov)) { implicit b => kafka =>
      * import GraphDSL.Implicits._
      * type In = ConsumerRecord[Array[Byte], java.lang.Integer]

      * val dummyProcessor = Flow[In].map { x =>    // tbd. probably way to make this without 'map' (i.e. run a 'Unit' producing function for its side effects)
      * val v: Int = x.value.toInt
      * Thread.sleep(1000)    // ms
      * println(v)
      * received += v
      * x
      * }

      * kafka.messages ~> dummyProcessor ~> Consumer.record2commit ~> kafka.commit
      * SourceShape(kafka.confirmation)
      * }

      * val control =
      * Source.fromGraph(graph)
      * .mapAsync(8)(identity)
      * .to(shutdownAsOnComplete)
      * .run()

      * sys.addShutdownHook {
      * control.stop()

      * println("Waiting for stop!")
      * Await.result(actorSystem.whenTerminated, 30.seconds)
      * println("AS stopped!")
      * }
    ***/

    received should contain theSameElementsInOrderAs(data)
  }
}

