package test

import java.util.UUID

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import com.softwaremill.react.kafka2._
import org.apache.kafka.common.serialization._
import org.scalatest.{FlatSpec, Matchers}

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

  val topic = "akauppi-" + UUID.randomUUID.toString   // e.g. "d8a630f0-093e-4022-b6f4-8fb1d8215f58"

  info(s"Using topic: $topic")

  val data: Seq[Int] = testData

  //---
  behavior of "Approach A"

  //merged to one scenario to be sure in order of execution
  it should "Be able to read data that was written in kafka" in {

    {
      implicit val actorSystem = ActorSystem("ApproachA_write")
      implicit val materializer = ActorMaterializer(
        ActorMaterializerSettings(actorSystem)
      )

      val keySerializer = new ByteArraySerializer // Note: no idea why this, reactive-kafka 'DummyProducer' sample uses it
      val valSerializer = new IntegerSerializer

      val producer = Producer(
        ProducerProvider(host, keySerializer, valSerializer)
      )

      val graph = Source.fromIterator(() => data.toIterator)
        .map(new java.lang.Integer(_))
        .via(Producer.value2record(topic)) // converts to 'ProducerRecord[Array[Byte], V]' (note: if using keys, replace this with our own, explicit mapping)
        .via(producer) // actually writes to Kafka (passes on a Future for success)
        .mapAsync(1)(identity) // waits for the Future, so one more value is pushed (an Akka Streams thing)
        .toMat(Sink.lastOption)(Keep.right) // Way to wait for steam end
        .run()

      // Now graph started and we able to run below. But, we should wait for graph to be competed.
      Await.result(graph, 10 seconds)
      actorSystem.terminate()
      println("Written to Kafka")
    }
    {
      implicit val actorSystem = ActorSystem("ApproachA_read")
      implicit val materializer = ActorMaterializer(
        ActorMaterializerSettings(actorSystem)
      )

      val keyDeserializer = new ByteArrayDeserializer
      val valDeserializer = new IntegerDeserializer

      val prov = ConsumerProvider(host, keyDeserializer, valDeserializer)
        .setup(TopicSubscription(topic))
        .groupId(consumerGroupId)
        .autoCommit(true)
        .props("auto.offset.reset" -> "earliest")   // In case of abscence of offset in kafka from what position should we start?

      val receivedFuture = Consumer
        .source(prov)
        .take(100) // this is dirty and will not catch situation when we publish 200 messages instead of 100. But the nature of kafka does not support end of stream
        .map(x => x.value.toInt)
        .toMat(Sink.fold(Seq.empty[Int])(_ :+ _))(Keep.right)
        .run()

      val received = Await.result(receivedFuture, 10 seconds)

      println(received)
      actorSystem.terminate()

      received shouldBe data
    }
  }
}

