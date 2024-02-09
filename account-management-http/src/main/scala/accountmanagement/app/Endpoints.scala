package accountmanagement.app
import accountmanagement.actor.AccountEventSourced
import sttp.client3.UriContext

import scala.util.Try

//import accountmanagement.app.AccountESApp.config
import accountmanagement.behavior.AccountESBehavior
import accountmanagement.behavior.AccountESBehavior.{ AccountES, behavior }
import accountmanagement.models.AccountManagementProtocol.{ AccountInfo, Transaction, TransactionHistory }
import com.devsisters.shardcake.{
  Config,
  GrpcConfig,
  GrpcPods,
  GrpcShardingService,
  KryoSerialization,
  Messenger,
  RedisConfig,
  ShardManagerClient,
  Sharding,
  StorageRedis
}
import com.devsisters.shardcake.interfaces.Serialization
import dev.profunktor.redis4cats.RedisCommands
import infra.Layers
import infra.Layers.ActorSystemZ
import sttp.client3.UriContext
import zio._
import zio.actors.Supervisor
import zio.cli._
import zio.schema._
import zio.schema.annotation.description
import zio.http.Header.Location
import zio.http._
import zio.http.codec._
import zio.http.endpoint.cli._
import zio.http.endpoint.{ Endpoint, EndpointExecutor }
import zio.prelude.Newtype

trait Endpoints {

  import zio.http.codec.PathCodec._

  import HttpCodec._

  val getAccount =
    Endpoint(Method.GET / "account" / string("accountId") ?? Doc.p("The unique identifier of the Account"))
      .out[Option[AccountInfo]] ?? Doc.p("Get an account by ID")

  val getTransactionHistory =
    Endpoint(
      Method.GET / "transaction" / "history" / string("accountId") ?? Doc.p("The unique identifier of the Account")
    )
      .out[TransactionHistory] ?? Doc.p("Get the transaction history by accountId")

  val createTransaction =
    Endpoint(Method.POST / "transaction")
      .in[Transaction]
      .out[AccountInfo] ?? Doc.p("Create a new transaction")
}

trait Handlers {
  import AccountESBehavior.AccountESMessage._
  type ActorEnv = Sharding with ActorSystemZ with Scope with Serialization with RedisCommands[Task, String, String]
  type AccountManager = Messenger[AccountESBehavior.AccountESMessage]
  object AccountManager {
    val live: ZLayer[ActorEnv, Throwable, AccountManager] = ZLayer {
      for {
        _              <- Sharding.registerEntity(AccountES, behavior)
        _              <- Sharding.registerScoped
        accountManager <- Sharding.messenger(AccountES)
      } yield accountManager
    }
  }
  def getAccountHandler(id: String): ZIO[AccountManager, Nothing, Option[AccountInfo]] = for {
    accountManager <- ZIO.service[AccountManager]
    accountInfo    <- accountManager.send(id)(replier => Get(replier)).orDie
  } yield accountInfo

  def createTransactionHandler(
      tx: Transaction
  ): ZIO[AccountManager, Throwable, AccountInfo] = for {
    accountManager <- ZIO.service[AccountManager]
    accountInfo <- accountManager.send(tx.accountId)(replier =>
      ApplyTransaction(AccountEventSourced.Transaction(tx.amount, tx.description), replier)
    )
    result <- ZIO.fromTry(accountInfo).orDie
  } yield result
}

//object TestCliApp extends zio.cli.ZIOCliDefault with Endpoints {
//  val cliApp =
//    HttpCliApp
//      .fromEndpoints(
//        name = "account-mgmt",
//        version = "0.0.1",
//        summary = HelpDoc.Span.text("Account management CLI"),
//        footer = HelpDoc.p("Copyright 2024"),
//        host = "localhost",
//        port = 8080,
//        endpoints = Chunk(getAccount, createTransaction, getTransactionHistory),
//        cliStyle = true
//      )
//      .cliApp
//}

//object TestCliClient extends zio.ZIOAppDefault with Endpoints {
//  val run =
//    clientExample
//      .provide(
//        EndpointExecutor.make(serviceName = "test"),
//        Client.default,
//        Scope.default
//      )
//
//  lazy val clientExample: URIO[EndpointExecutor[Unit] & Scope, Unit] =
//    for {
//      executor <- ZIO.service[EndpointExecutor[Unit]]
//      _        <- executor(getAccount(42, Location.parse("some-location").toOption.get)).debug("result1")
//      _        <- executor(getUserPosts(42, 200, "adam")).debug("result2")
//      _        <- executor(createUser(User(2, "john", Some("john@test.com")))).debug("result3")
//    } yield ()
//
//}
