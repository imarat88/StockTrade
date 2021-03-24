import org.scalatest.funspec.AnyFunSpec
import org.scalatest.funsuite.AnyFunSuite
import StockTradeBoot._
import org.scalatest.BeforeAndAfterEach

import java.io.{File, PrintWriter, StringWriter}
import scala.collection.mutable
import scala.io.Source
class StockTradeTest extends AnyFunSuite with BeforeAndAfterEach {

  test("should successfully read from input file with provided name and write clientWallet data to provided output file") {
    val clientSource = Source.fromFile(getClass.getClassLoader.getResource("clients1.txt").getPath)
    val clientsWallet = readClient(clientSource)
    val resultsFilePath = getClass.getClassLoader.getResource("clients1.txt").getPath.replace("clients1.txt","results1.txt")

    assert(clientsWallet.size>0)
    val writer = new PrintWriter(new File(resultsFilePath))
    printResults(writer,clientsWallet)
    writer.close()
    val resultSource = Source.fromFile(getClass.getClassLoader.getResource("results1.txt").getPath)

    val checkClientWallet = readClient(resultSource)
    assert(clientsWallet.equals(checkClientWallet))
  }

  test("should properly give priority according to order of request") {
    import StockTradeBoot._
    val inputClients = """C1  1000    10  5   15  0
                         |C2  2000    200   35  40  10
                         |C3  2000    15   35  40  10""".stripMargin
    val inputOrders = """C1   b   A   10  10
                        |C2   b   A   10  10
                        |C3   s   A   10  15""".stripMargin
    val expectedResults = """C1	900	20	5	15	0
                            |C2	1950	205	35	40	10
                            |C3	2150	0	35	40	10
                            |""".stripMargin.replaceAll("\r","")
    val clientSource = Source.fromString(inputClients)
    val orderSource = Source.fromString(inputOrders)
    init(clientSource)
    processOrders(orderSource)
    var writer = new StringWriter()
    printResults(writer,StockTradeBoot.clientMap)
    assert(writer.toString.equals(expectedResults))
    writer.close()
  }
  test("should skip orders without resources") {
    import StockTradeBoot._
    val inputClients = """C1  1000    10  5   15  0
                         |C2  2000    200   35  40  10
                         |C3  2000    15   35  40  10""".stripMargin
    val inputOrders = """C1   b   A   10  10
                        |C2   b   A   10  10
                        |C3   s   A   10  150""".stripMargin
    val expectedResults = """C1	1000	10	5	15	0
                            |C2	2000	200	35	40	10
                            |C3	2000	15	35	40	10
                            |""".stripMargin.replaceAll("\r","")
    val clientSource = Source.fromString(inputClients)
    val orderSource = Source.fromString(inputOrders)
    init(clientSource)
    processOrders(orderSource)
    var writer = new StringWriter()
    printResults(writer,StockTradeBoot.clientMap)
    assert(writer.toString.equals(expectedResults))
    writer.close()
  }
}
