package test

import akka.actor.ActorSystem
import akka.stream.{SourceShape, ActorMaterializer}
import akka.stream.scaladsl.{Flow, GraphDSL, Sink, Source}
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.scalatest.{FlatSpec, Matchers}
import org.apache.kafka.common.serialization._
import com.softwaremill.react.kafka2._
import java.util.UUID

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Failure

class ApproachA extends FlatSpec with Matchers {
  import ApproachA._

  implicit val actorSystem = ActorSystem("ReactiveKafka")
  implicit val materializer = ActorMaterializer()

  val topic = UUID.randomUUID.toString   // e.g. "d8a630f0-093e-4022-b6f4-8fb1d8215f58"

  info(s"Using topic: $topic")

  val data: Seq[Int] = testData
  val dataSource: Source[Int,_] = Source(data toVector)

  //---
  behavior of "Approach A"

  it should "Be able to write to Kafka" in {

    val keySerializer = new ByteArraySerializer // Note: no idea why this, reactive-kafka 'DummyProducer' sample uses it
    val valSerializer = new IntegerSerializer

    val producerProvider = ProducerProvider(host, keySerializer, valSerializer)

    dataSource
      .map(new java.lang.Integer(_))
      .via(Producer.value2record(topic))
      .via(Producer(producerProvider))
      .mapAsync(1)(identity)
      .to(shutdownAsOnComplete)
      .run()
  }

  it should "Be able to read from Kafka" ignore /*in*/ {

    val keyDeserializer = new ByteArrayDeserializer
    val valDeserializer = new IntegerDeserializer

    val consumerProvider = ConsumerProvider(host, keyDeserializer, valDeserializer)
      .setup(TopicSubscription(topic))
      .groupId(consumerGroupId)
      .autoCommit(true)
      .props("auto.offset.reset" -> "earliest") // as a sample

    // TBD: how to read from the beginning?

    // tbd. Compare that the output is same as 'testData'

    val graph = GraphDSL.create(Consumer[Array[Byte], java.lang.Integer](consumerProvider)) { implicit b => kafka =>
      import GraphDSL.Implicits._
      type In = ConsumerRecord[Array[Byte], java.lang.Integer]
      val dummyProcessor = Flow[In].map { x => Thread.sleep(100); println(x); x }

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
  }
}


object ApproachA {

  def shutdownAsOnComplete[T](implicit as: ActorSystem) = Sink.onComplete[T] {
    case Failure(ex) =>
      println("Stream finished with error")
      ex.printStackTrace()
      as.terminate()
      println("Terminate AS.")
    case _ =>
      println("Stream finished successfully")
      as.terminate()
      println("Terminate AS.")
  }
}
