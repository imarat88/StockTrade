import scala.collection.mutable
import scala.io.Source
import scala.collection.mutable.{Map, TreeMap}
import java.io.{File, PrintWriter, Writer}

case class OrderDetail(
                        clientName: String,
                        order: Int,
                        operation: String,
                        price: Int,
                        var qty: Int
                      )
case class OrderCompareMap(
                            order: Int,
                            price: Int,
                            operation: String
                          )
case class ClientWallet (
                        var money: Int,
                        stocks:mutable.Map[String, Int]
                        )

object CompareOrderDetailsAsc extends Ordering[OrderCompareMap]{
  override def compare(x: OrderCompareMap, y: OrderCompareMap): Int = x.price compare(y.price) match {
    case v if v != 0 => v
    case v if v == 0 => x.order compare y.order
  }
}

object CompareOrderDetailsDesc extends Ordering[OrderCompareMap]{
  override def compare(x: OrderCompareMap, y: OrderCompareMap): Int = y.price compare(x.price) match {
    case v if v != 0 => v
    case v if v == 0 => x.order compare y.order
  }
}

object CompareOrderDetailsByOrder extends Ordering[OrderCompareMap]{
  override def compare(x: OrderCompareMap, y: OrderCompareMap): Int = x.order compare(y.order)
}

object StockTradeBoot extends App {

  def balanceWallet(wallet: ClientWallet, money: Int, stockName: String, qty: Int) = {
    wallet.money += money
    wallet.stocks(stockName)+=qty
  }

  def closeOrder(
                  sellerDetail: OrderDetail,
                  buyerDetail: OrderDetail,
                  qty: Int,
                  price: Int,
                  stockName: String) = {
    sellerDetail.qty -= qty
    buyerDetail.qty -= qty
    balanceWallet(clientMap(sellerDetail.clientName), qty*price,stockName, -qty)
    balanceWallet(clientMap(buyerDetail.clientName), -qty*price, stockName, qty)
  }

  def operateRequest(
                      stockName: String,
                      orderKey: OrderCompareMap,
                      sellOrders: Map[String,mutable.TreeMap[OrderCompareMap, OrderDetail]],
                      buyOrders:Map[String,mutable.TreeMap[OrderCompareMap, OrderDetail]]
                    ):Boolean = {

    orderKey.operation match {
      // selling operation
      case "s" => {
        val sellingOrder = sellOrders(stockName).get(orderKey).get
        if (clientMap(sellingOrder.clientName).stocks(stockName) < sellingOrder.qty) {
          sellOrders(stockName).remove(orderKey)
          return false
        }
        while (buyOrders(stockName).size>0 && buyOrders(stockName).head._2.price>=sellingOrder.price) {
          val buyingOrderKey = buyOrders(stockName).head._1
          val buyingOrder = buyOrders(stockName).head._2

          //if element from sequence starts to buy cheaper then break and proceed with other requests in stack
          if (sellingOrder.price>buyingOrder.price) {
            return false
          }
          val qtyAbleToBuy = Math.min(buyingOrder.qty, sellingOrder.qty)
          closeOrder(
            sellerDetail = sellOrders(stockName).get(orderKey).get,
            buyerDetail = buyOrders(stockName).get(buyingOrderKey).get,
            qty = qtyAbleToBuy,
            price = buyingOrder.price,
            stockName = stockName
          )
          if (buyOrders(stockName).get(buyingOrderKey).get.qty == 0) {
            //removing closed order from buyer
            buyOrders(stockName).remove(buyingOrderKey)
          }
          if (sellOrders(stockName).get(orderKey).get.qty == 0) {
            //removing closed order from seller
            sellOrders(stockName).remove(orderKey)
            return true
          }
        }
        return false
      }
      case "b" => {
        val buyingOrder = buyOrders(stockName).get(orderKey).get
        if (clientMap(buyingOrder.clientName).money < buyingOrder.price * buyingOrder.qty) {
          buyOrders(stockName).remove(orderKey)
          return false
        }
        while (sellOrders(stockName).size > 0 && sellOrders(stockName).head._2.price <= buyingOrder.price) {
          val sellingOrderKey = sellOrders(stockName).head._1
          val sellingOrder = sellOrders(stockName).head._2

          //if element from sequence starts to buy cheaper then break and proceed with other requests in stack
          if (sellingOrder.price > buyingOrder.price) {
            return false
          }
          val qtyAbleToBuy = Math.min(buyingOrder.qty, sellingOrder.qty)
          val buyingPrice = Math.min(buyingOrder.price, sellingOrder.price)
          closeOrder(
            sellerDetail = sellOrders(stockName).get(sellingOrderKey).get,
            buyerDetail = buyOrders(stockName).get(orderKey).get,
            qty = qtyAbleToBuy,
            price = buyingPrice,
            stockName = stockName
          )
          if (buyOrders(stockName).get(orderKey).get.qty == 0) {
            //removing closed order from buyer
            buyOrders(stockName).remove(orderKey)
            return true
          }
          if (sellOrders(stockName).get(sellingOrderKey).get.qty == 0) {
            //removing closed order from seller
            sellOrders(stockName).remove(sellingOrderKey)
          }
        }
        return false
      }
    }
  }

  def readClient(source: Source) = {
    val stockNames = List("A","B","C","D")
    source.getLines()
      .map{
        v => {
          val clientData = v.split("\\s+").toList
          val clientName = clientData(0)
          val money = clientData(1).toInt
          val stockMap = stockNames.zip(clientData.drop(2).map(_.toInt))
          clientName -> ClientWallet(
            money = money,
            stocks = mutable.Map.from[String, Int](stockMap)
          )
        }
      }.toMap
  }

  def processOrders(source: Source) = {
    source.getLines()
      .foreach{
        v => {
          // orderData: client operation stockName price qty
          val orderData = v.split("\\s+")
          val stockName = orderData(2)
          val operation = orderData(1)
          val orderDetail = OrderDetail(
            clientName = orderData(0),
            order = order,
            operation = operation,
            price = orderData(3).toInt,
            qty = orderData(4).toInt
          )
          val orderKey = OrderCompareMap(order, orderDetail.price, operation)
          operation match {
            case "b" => {
              buyingSet(stockName).addOne(orderKey,orderDetail)
              operateRequest(stockName, orderKey, sellingSet, buyingSet)
            }
            case "s" => {
              sellingSet(stockName).addOne(orderKey,orderDetail)
              operateRequest(stockName, orderKey, sellingSet, buyingSet)
            }
          }
          order+=1
        }
      }
  }

  def printResults(writer: Writer, clientsWallet:scala.collection.immutable.Map[String, ClientWallet]) = {

    clientsWallet.foreach{
      v =>
        writer.write(
          s"${v._1}\t${v._2.money}\t${v._2.stocks("A")}\t${v._2.stocks("B")}\t${v._2.stocks("C")}\t${v._2.stocks("D")}\n"
        )
    }
    writer
  }

  def init(clientSource: Source) = {
    resultsFilePath = getClass.getResource(clientsFile).getPath.replace(clientsFile,resultsFile)
    stockNames = List("A","B","C","D")
    buyingSet = mutable.Map.from(stockNames.map(v => v->mutable.TreeMap[OrderCompareMap, OrderDetail]()(CompareOrderDetailsDesc)))
    sellingSet = mutable.Map.from(stockNames.map(v => v->mutable.TreeMap[OrderCompareMap, OrderDetail]()(CompareOrderDetailsAsc)))
    order = 0
    clientMap = readClient(clientSource)
  }

  var clientsFile:String = "clients.txt"
  var ordersFile:String = "orders.txt"
  var resultsFile:String = "results.txt"
  var resultsFilePath:String = _
  var stockNames:List[String] = _
  var buyingSet:Map[String,mutable.TreeMap[OrderCompareMap, OrderDetail]] = _
  var sellingSet:Map[String,mutable.TreeMap[OrderCompareMap, OrderDetail]] = _
  var order:Int = _
  var clientMap: scala.collection.immutable.Map[String, ClientWallet] = _
  val clientSource = Source.fromFile(getClass.getClassLoader.getResource(clientsFile).getPath)
  val orderSource = Source.fromFile(getClass.getClassLoader.getResource(ordersFile).getPath)

  init(clientSource)
  processOrders(orderSource)
  val writer = new PrintWriter(new File(resultsFilePath))
  printResults(writer, clientMap)
  writer.close()
}
