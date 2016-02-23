package test

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer}
import akka.stream.scaladsl.{RunnableGraph, Sink, Source}
import com.softwaremill.react.kafka.KafkaMessages._
import com.softwaremill.react.kafka.{ConsumerProperties, ProducerMessage, ReactiveKafka, ProducerProperties}
import org.reactivestreams.{Publisher, Subscriber}
import org.scalatest.{FlatSpec, Matchers}
import org.apache.kafka.common.serialization._
import java.util.UUID

class ApproachB extends FlatSpec with Matchers {
  import ApproachB._

  implicit val actorSystem = ActorSystem("ApproachB")
  implicit val materializer = ActorMaterializer()

  val kafka = new ReactiveKafka

  val topic = UUID.randomUUID.toString   // e.g. "d8a630f0-093e-4022-b6f4-8fb1d8215f58"

  info(s"Using topic: $topic")

  // test data
  //
  val data: Seq[Int] = testData
  val dataSource: Source[Int,_] = Source(data toVector)

  //---
  behavior of "Approach B"

  it should "Be able to write to Kafka" in {

    // tbd. What does the term 'Subscriber' here really mean? AKa230216
    //
    val subscriber: Subscriber[StringProducerMessage] = kafka.publish( ProducerProperties(
      bootstrapServers = host,
      topic = topic,
      valueSerializer = new StringSerializer()
    ))

    val graph: RunnableGraph[_] = dataSource
      .map( n => ProducerMessage(n.toString) )
      .to( Sink.fromSubscriber(subscriber) )

    graph.run()

    println("Written data to Kafka")

    // tbd. Should we wait here?
  }

  it should "Be able to read from Kafka" ignore /*in*/ {

    val publisher: Publisher[StringConsumerRecord] = kafka.consume(ConsumerProperties(
      bootstrapServers = host,
      topic = topic,
      groupId = consumerGroupId,
      valueDeserializer = new StringDeserializer()
    ))

    Source.fromPublisher(publisher).runForeach(println)

    // tbd. How to best wait here until we've gotten the data (e.g. 'data.length' values)? AKa230216

    // tbd. Compare that the output is same as 'testData'

    println("Read data from Kafka")
  }
}


object ApproachB {

  /***
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
  ***/
}
