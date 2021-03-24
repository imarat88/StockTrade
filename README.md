# StockTrade
StockTrade emulation

Methods:

# init(clientSource: Source)
Initializes global variables of the application.   
clientSource is a source that is used for initializing a global variable **clientMap** which gets it's data from **readClient** method inside of init().

# readClient(clientSource: Source):Map[String, ClientWallet]  

**readClient** is a method that reads client data from *clients.txt* file and returns Map[String, ClientWallet]  
where the Key is client's name and ClientWallet has following model:

case class ClientWallet (  
                        var money: Int,  
                        stocks:mutable.Map[String, Int]  
                        )  
Where stocks is a map of stock name and it's quantity.

# def processOrders(source: Source): Unit

**processOrders** is a method that iterates over given source data(*orders.txt*)  
and processed orders.

# def operateRequest  

_**parameters**_:  
**(  
stockName: String,  
orderKey: OrderCompareMap,  
sellOrders: Map[String,mutable.TreeMap[OrderCompareMap, OrderDetail]],  
buyOrders:Map[String,mutable.TreeMap[OrderCompareMap, OrderDetail]]  
)**  
**returns**: Boolean  
**operateRequest** is a method that is responsible of matching the given order with trading orders in list.  
*orderKey* is a unique key of the processing order of the type OrderCompareMap(  
                            order: Int,  
                            price: Int,  
                            operation: String  
                          ).    
*sellOrders* is a map of stock lists where key is a stock name and a TreeMap lists suspended selling orders in pool of the given stock name.  
*buyOrders* is a map of stock lists where key is a stock name and a TreeMap lists suspended buying orders in pool of the given stock name.


# def printResults(writer: Writer, clientsWallet:Map[String, ClientWallet]): Writer  
printResults is a method that prints resulting clientsWallet into output Writer source(*results.txt*).
