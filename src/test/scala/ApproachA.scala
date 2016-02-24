package test

import akka.actor.ActorSystem
import akka.stream.{SourceShape, ActorMaterializer}
import akka.stream.scaladsl.{Flow, GraphDSL, Sink, Source}
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
*/
class ApproachA extends FlatSpec with Matchers {

  implicit val actorSystem = ActorSystem("ApproachA")
  implicit val materializer = ActorMaterializer()

  val topic = UUID.randomUUID.toString   // e.g. "d8a630f0-093e-4022-b6f4-8fb1d8215f58"

  info(s"Using topic: $topic")

  val data: Seq[Int] = testData

  //---
  behavior of "Approach A"

  it should "Be able to write to Kafka" in {

    val keySerializer = new ByteArraySerializer // Note: no idea why this, reactive-kafka 'DummyProducer' sample uses it
    val valSerializer = new IntegerSerializer

    val prov = ProducerProvider(host, keySerializer, valSerializer)

    Source(data toVector)
      .map(new java.lang.Integer(_))
      .via(Producer.value2record(topic))      // converts to 'ProducerRecord[Array[Byte], V]' (note: if using keys, replace this with our own, explicit mapping)
      .via(Producer(prov))                    // Q: this the place that actually writes to Kafka?
      .mapAsync(1)(identity)                  // Q: what does this do?
      .to(shutdownAsOnComplete)               // Q: and this?
      .run()
  }

  it should "Be able to read from Kafka" in {

    val keyDeserializer = new ByteArrayDeserializer
    val valDeserializer = new IntegerDeserializer

    val prov = ConsumerProvider(host, keyDeserializer, valDeserializer)
      .setup(TopicSubscription(topic))
      .groupId(consumerGroupId)
      .autoCommit(false)
      .props("auto.offset.reset" -> "earliest")   // tbd. what's the purpose of this?

    var received = new ArrayBuffer[Int]

    val graph = GraphDSL.create(Consumer[Array[Byte], java.lang.Integer](prov)) { implicit b => kafka =>
      import GraphDSL.Implicits._
      type In = ConsumerRecord[Array[Byte], java.lang.Integer]

      val dummyProcessor = Flow[In].map { x =>    // tbd. probably way to make this without 'map' (i.e. run a 'Unit' producing function for its side effects)
        val v: Int = x.value.toInt
        Thread.sleep(100)
        println(v)
        received += v
        x
      }

      kafka.messages ~> dummyProcessor ~> Consumer.record2commit ~> kafka.commit
      SourceShape(kafka.confirmation)
    }

    val control =
      Source.fromGraph(graph)
        .mapAsync(8)(identity)
        .to(shutdownAsOnComplete)
        .run()

    sys.addShutdownHook {
      control.stop()

      println("Waiting for stop!")
      Await.result(actorSystem.whenTerminated, 30.seconds)
      println("AS stopped!")
    }

    received should contain theSameElementsInOrderAs(data)
  }
}

