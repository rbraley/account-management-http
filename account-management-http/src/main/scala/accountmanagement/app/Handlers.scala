package accountmanagement.app

import accountmanagement.actor.AccountEventSourced
import accountmanagement.behavior.AccountESBehavior
import accountmanagement.behavior.AccountESBehavior.{ AccountES, behavior }
import AccountManagementProtocol.{ AccountInfo, InsufficientFundsError, Transaction, TransactionHistory }
import com.devsisters.shardcake.interfaces.Serialization
import com.devsisters.shardcake.{ Messenger, Sharding }
import dev.profunktor.redis4cats.RedisCommands
import infra.Layers.ActorSystemZ
import zio.{ Scope, Task, ZIO, ZLayer }

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
  ): ZIO[AccountManager, InsufficientFundsError, AccountInfo] = (for {
    accountManager <- ZIO.service[AccountManager]
    accountInfo <- accountManager.send(tx.`account_id`)(replier =>
      ApplyTransaction(AccountEventSourced.Transaction(tx.amount, tx.description), replier)
    )
    result <- ZIO
      .fromTry(accountInfo)
  } yield result).mapError {
    case t: Throwable if t.getMessage.contains("Insufficient") => InsufficientFundsError()
  }
  def getTransactionHistoryHandler(id: String): ZIO[AccountManager, Nothing, TransactionHistory] = for {
    accountManager <- ZIO.service[AccountManager]
    accountInfo    <- accountManager.send(id)(replier => GetTransactionHistory(replier)).orDie
  } yield accountInfo
}
