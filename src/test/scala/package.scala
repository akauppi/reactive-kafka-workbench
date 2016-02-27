import akka.actor.ActorSystem
import akka.stream.scaladsl.Sink

import scala.util.Failure

// Joined configuration (and tools) for all approaches

package object test {
  val host = "localhost:9092"
  val consumerGroupId = "sample"

  def testData: Seq[Int] = {
    //Array.fill(100) { scala.util.Random.nextInt(1000) }
    (1 to 100).toSeq
  }

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
