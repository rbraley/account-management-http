package accountmanagement.app

import accountmanagement.actor.AccountEventSourced
import accountmanagement.actor.AccountManager.AccountManager
import accountmanagement.app.AccountManagementProtocol.{
  AccountInfo,
  InsufficientFundsError,
  Transaction,
  TransactionHistory
}
import accountmanagement.behavior.AccountESBehavior
import zio.ZIO

/*
 *  Handlers contains implementations for our endpoints as described in Endpoints.scala
 *  These handlers depend on an AccountManager so they can delegate to our event sourced AccountManager entities.
 */
trait Handlers {
  import AccountESBehavior.AccountESMessage._

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
