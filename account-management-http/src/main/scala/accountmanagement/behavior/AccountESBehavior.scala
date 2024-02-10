package accountmanagement.behavior

import accountmanagement.actor.AccountEventSourced
import accountmanagement.actor.AccountEventSourced.Transaction
import accountmanagement.app.AccountManagementProtocol
import AccountManagementProtocol.{ AccountInfo, TransactionHistory }
import com.devsisters.shardcake.Messenger.Replier
import com.devsisters.shardcake.{ EntityType, Sharding }
import infra.Layers.ActorSystemZ
import zio.{ Dequeue, RIO, ZIO }

import scala.util.Try

object AccountESBehavior {
  sealed trait AccountESMessage

  object AccountESMessage {
    case class ApplyTransaction(tx: Transaction, replier: Replier[Try[AccountInfo]]) extends AccountESMessage
    case class Get(replier: Replier[Option[AccountInfo]])                            extends AccountESMessage
    case class GetTransactionHistory(replier: Replier[TransactionHistory])           extends AccountESMessage
  }

  object AccountES extends EntityType[AccountESMessage]("accountES")

  def behavior(
      entityId: String,
      messages: Dequeue[AccountESMessage]
  ): RIO[Sharding with ActorSystemZ, Nothing] =
    ZIO.logInfo(s"Started entity $entityId") *>
      messages.take.flatMap(handleMessage(entityId, _)).forever

  def handleMessage(
      entityId: String,
      message: AccountESMessage
  ): RIO[Sharding with ActorSystemZ, Unit] =
    AccountEventSourced
      .actorRef(entityId)
      .flatMap { actor =>
        message match {
          case AccountESMessage.ApplyTransaction(userId, replier) =>
            actor
              .?(AccountEventSourced.ApplyTransaction(userId))
              .flatMap { accountInfo =>
                replier.reply(accountInfo)
              }
          case AccountESMessage.Get(replier) =>
            actor
              .?(AccountEventSourced.Get)
              .flatMap(accountState =>
                if (accountState.txs.isEmpty)
                  replier.reply(None)
                else
                  replier.reply(Some(AccountInfo(entityId, accountState.balance, accountState.userDetails)))
              )
          case AccountESMessage.GetTransactionHistory(replier) =>
            actor
              .?(AccountEventSourced.Get)
              .flatMap(accountState =>
                replier.reply(
                  TransactionHistory(
                    entityId,
                    accountState.txs
                      .map(tx => AccountManagementProtocol.Transaction(entityId, tx.amount, tx.description))
                  )
                )
              )
        }
      }
}
