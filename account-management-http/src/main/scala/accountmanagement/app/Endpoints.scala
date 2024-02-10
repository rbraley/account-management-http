package accountmanagement.app
import AccountManagementProtocol.InsufficientFundsError
import AccountManagementProtocol.{ AccountInfo, Transaction, TransactionHistory }
import zio.http.Method._
import zio.http.codec._
import zio.http.endpoint.Endpoint

trait Endpoints {
  import zio.http.codec.PathCodec._

  val getAccount =
    Endpoint(GET / "account" / string("accountId") ?? Doc.p("The unique identifier of the Account"))
      .out[Option[AccountInfo]] ?? Doc.p("Get an account by ID")

  val getTransactionHistory =
    Endpoint(GET / "transaction" / "history" / string("accountId") ?? Doc.p("The unique identifier of the Account"))
      .out[TransactionHistory] ?? Doc.p("Get the transaction history by accountId")

  val createTransaction =
    Endpoint(POST / "transaction")
      .in[Transaction]
      .outError[InsufficientFundsError](zio.http.Status.UnprocessableEntity, Doc.p("Insufficient Funds"))
      .out[AccountInfo] ?? Doc.p("Create a new transaction")
}
