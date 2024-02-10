package accountmanagement.app
import AccountManagementProtocol.{ AccountInfo, Transaction, TransactionHistory }
import accountmanagement.app.DomainErrors.{ InsufficientFundsError, AccountNotFound }
import zio.http.Method._
import zio.http.codec._
import zio.http.endpoint.Endpoint

/*
 * Endpoints contains pure descriptions of the HTTP APIs we will expose as data structures
 * Note that these are mere descriptions and it does not implement them.
 * The implmentations are done in Handlers.scala
 */
trait Endpoints {
  import zio.http.codec.PathCodec._

  val getAccount =
    Endpoint(GET / "account" / string("accountId") ?? Doc.p("The unique identifier of the Account"))
      .outError[AccountNotFound](zio.http.Status.NotFound, Doc.p("No deposits have been made to this account"))
      .out[AccountInfo] ?? Doc.p("Get an account by ID")

  val getTransactionHistory =
    Endpoint(GET / "transaction" / "history" / string("accountId") ?? Doc.p("The unique identifier of the Account"))
      .outError[AccountNotFound](zio.http.Status.NotFound, Doc.p("No deposits have been made to this account"))
      .out[TransactionHistory] ?? Doc.p("Get the transaction history by accountId")

  val createTransaction =
    Endpoint(POST / "transaction")
      .in[Transaction]
      .outError[InsufficientFundsError](zio.http.Status.UnprocessableEntity, Doc.p("Insufficient Funds"))
      .out[AccountInfo] ?? Doc.p("Create a new transaction")
}
