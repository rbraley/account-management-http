package accountmanagement.behavior

import com.devsisters.shardcake.Messenger.Replier
import com.devsisters.shardcake.{ EntityType, Sharding }
import accountmanagement.actor.AccountEventSourced
import accountmanagement.actor.AccountEventSourced.Transaction
import zio.{ Dequeue, RIO, ZIO }

import scala.util.Try
import infra.Layers.ActorSystemZ

object AccountESBehavior {
  sealed trait AccountESMessage

  object AccountESMessage {
    case class ApplyTransaction(tx: Transaction, replier: Replier[Try[BigDecimal]]) extends AccountESMessage
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
              .flatMap { balance =>
                replier.reply(balance)
              }
        }
      }
}
