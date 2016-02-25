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

import scala.collection.mutable.ArrayBuffer

/*
* Approach following what's on the front page of reactive-kafka GitHub Repo:
*   -> https://github.com/softwaremill/reactive-kafka#example-usage
*
* Note: I don't actually want to deal with the 'Subscriber' and 'Producer' stuff. Just give me streams,
*       'Source', 'Flow' and 'Sink'. AKa230216
*/
class ApproachBTest extends FlatSpec with Matchers {

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

  it should "Be able to write to Kafka" ignore /*in*/ {

    val subscriber: Subscriber[StringProducerMessage] = kafka.publish( ProducerProperties(
      bootstrapServers = host,
      topic = topic,
      valueSerializer = new StringSerializer
    ))

    val graph: RunnableGraph[_] = dataSource
      .map( n => ProducerMessage(n.toString) )
      .to( Sink.fromSubscriber(subscriber) )

    graph.run()

    // tbd. Should we wait here?

    println("Written data to Kafka - waiting...")

    Thread.sleep(10000) // ms

    println("Wait passed")
  }

  it should "Be able to read from Kafka" ignore /*in*/ {

    val publisher: Publisher[StringConsumerRecord] = kafka.consume( ConsumerProperties(
      bootstrapServers = host,
      topic = topic,
      groupId = consumerGroupId,
      valueDeserializer = new StringDeserializer
    ))

    var received = new ArrayBuffer[Int]

    Source.fromPublisher(publisher).runForeach( x => {
      val v: Int = x.value.toInt
      println(v)
      received += v
    })

    // tbd. How to best wait here until we've gotten the data (e.g. 'data.length' values)? AKa230216

    received should contain theSameElementsInOrderAs(data)

    println("Read data from Kafka")
  }
}
