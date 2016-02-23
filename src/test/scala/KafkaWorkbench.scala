package test

import akka.actor.ActorSystem
import akka.stream.{SourceShape, ActorMaterializer}
import akka.stream.scaladsl.{Flow, GraphDSL, Sink, Source}
import com.softwaremill.react.kafka.KafkaMessages._
import com.softwaremill.react.kafka.{ConsumerProperties, ProducerMessage, ReactiveKafka, ProducerProperties}
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.reactivestreams.{Publisher, Subscriber}
import org.scalatest.time.{Second, Seconds, Span}
import org.scalatest.{FlatSpec, Matchers}
import org.apache.kafka.common.serialization._
import com.softwaremill.react.kafka2._
import java.util.UUID

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Failure

class KafkaWorkbench extends FlatSpec with Matchers {
  import KafkaWorkbench._

  implicit val actorSystem = ActorSystem("ReactiveKafka")
  implicit val materializer = ActorMaterializer()

  val kafka = new ReactiveKafka

  val host = "localhost:9092"

  val topic = UUID.randomUUID.toString   // e.g. "d8a630f0-093e-4022-b6f4-8fb1d8215f58"

  val consumerGroupId = "sample"

  info(s"Using topic: $topic")

  val APPROACH_B = true

  //---
  behavior of "SomeTest"

  // Generate an array of test data
  //
  val data: Seq[Int] = Array.fill(100) { scala.util.Random.nextInt(1000) }

  it should "Be able to write to Kafka" in {

    info( data.mkString(",") )

    if (APPROACH_B) {   // Approach B

      // Note: This with 'publish', 'Subscriber' etc. is VERY confusing. Do not blame yourself! AKa230216

      val subscriber: Subscriber[StringProducerMessage] = kafka.publish( ProducerProperties(
        bootstrapServers = host,
        topic = topic,
        valueSerializer = new StringSerializer()
      ))

      // This should write the data to the Kafka topic
      //
      Source(data toVector).map( n => ProducerMessage(n.toString) )
        .to( Sink.fromSubscriber(subscriber) ).run()

      println("Written data to Kafka")

    } else {    // Approach A (didn't work) AKa230216
      val keySerializer = new ByteArraySerializer // Note: no idea why this, reactive-kafka 'DummyProducer' sample uses it
      val valSerializer = new IntegerSerializer

      val producerProvider = ProducerProvider(host, keySerializer, valSerializer)

      Source(data toVector)
        .map(new java.lang.Integer(_))
        .via(Producer.value2record(topic))
        .via(Producer(producerProvider))
        .mapAsync(1)(identity)
        .to(shutdownAsOnComplete)
        .run()
    }
  }

  it should "Be able to read from Kafka" in {

    if (APPROACH_B) {
      val publisher: Publisher[StringConsumerRecord] = kafka.consume(ConsumerProperties(
        bootstrapServers = host,
        topic = topic,
        groupId = consumerGroupId,
        valueDeserializer = new StringDeserializer()
      ))

      Source.fromPublisher(publisher).runForeach(println)

      // tbd. How to best wait here until we've gotten the data (e.g. 'data.length' values)? AKa230216

      println("Read data from Kafka")

    } else {  // Approach A (did not work)}
      val keyDeserializer = new ByteArrayDeserializer
      val valDeserializer = new IntegerDeserializer

      val consumerProvider = ConsumerProvider(host, keyDeserializer, valDeserializer)
        .setup(TopicSubscription(topic))
        .groupId(consumerGroupId)
        .autoCommit(true)
        .props("auto.offset.reset" -> "earliest") // as a sample

      // TBD: how to read from the beginning?

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
}


object KafkaWorkbench {
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
