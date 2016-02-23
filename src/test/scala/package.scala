import akka.stream.scaladsl.Source

// Joined configuration (and tools) for all approaches

package object test {
  val host = "localhost:9092"
  val consumerGroupId = "sample"

  def testData: Seq[Int] = {
    Array.fill(100) { scala.util.Random.nextInt(1000) }
  }

  /*** disabled
  def testDataSource: Source[Int,_] = {
    val arr = Array.fill(100) {
      scala.util.Random.nextInt(1000)
    }
    Source(arr toVector)
  }
  ***/
}
