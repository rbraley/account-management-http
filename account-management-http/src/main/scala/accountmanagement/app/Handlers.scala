package accountmanagement.app

import accountmanagement.actor.AccountEventSourced
import accountmanagement.actor.AccountManager.AccountManager
import accountmanagement.app.AccountManagementProtocol.{ AccountInfo, Transaction, TransactionHistory }
import accountmanagement.app.DomainErrors.{ AccountNotFound, InsufficientFundsError }
import accountmanagement.behavior.AccountESBehavior
import zio.ZIO

/*
 *  Handlers contains implementations for our endpoints as described in Endpoints.scala
 *  These handlers depend on an AccountManager so they can delegate to our event sourced AccountManager entities.
 */
trait Handlers {
  import AccountESBehavior.AccountESMessage._

  def getAccountHandler(id: String): ZIO[AccountManager, AccountNotFound, AccountInfo] = {
    (for {
      accountManager <- ZIO.service[AccountManager]
      accountInfoOpt <- accountManager.send(id)(replier => Get(replier))
      accountInfo    <- ZIO.fromOption(accountInfoOpt)
    } yield accountInfo)
      .mapError { case None =>
        AccountNotFound()
      }
  }

  def createTransactionHandler(
      tx: Transaction
  ): ZIO[AccountManager, InsufficientFundsError, AccountInfo] = {
    (for {
      accountManager <- ZIO.service[AccountManager]
      accountInfo <- accountManager.send(tx.`account_id`)(replier =>
        ApplyTransaction(AccountEventSourced.Transaction(tx.amount, tx.description), replier)
      )
      result <- ZIO
        .fromTry(accountInfo)
    } yield result).mapError {
      case t: Throwable if t.getMessage.contains("Insufficient") => InsufficientFundsError()
    }
  }

  def getTransactionHistoryHandler(id: String): ZIO[AccountManager, AccountNotFound, TransactionHistory] = {
    for {
      accountManager <- ZIO.service[AccountManager]
      accountInfo    <- accountManager.send(id)(replier => GetTransactionHistory(replier)).orDie
      transactionHistory <-
        if (accountInfo.transactions.isEmpty) ZIO.fail(AccountNotFound()) else ZIO.succeed(accountInfo)
    } yield transactionHistory
  }

}
