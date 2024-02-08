package accountmanagement.behavior

import com.devsisters.shardcake.Messenger.Replier
import com.devsisters.shardcake.{ EntityType, Sharding }
import accountmanagement.actor.AccountEventSourced
import zio.{ Dequeue, RIO, ZIO }

import scala.util.Try
import infra.Layers.ActorSystemZ

object AccountESBehavior {
  sealed trait AccountESMessage

  object AccountESMessage {
    case class Join(userId: String, replier: Replier[Try[Set[String]]]) extends AccountESMessage
    case class Leave(userId: String)                                    extends AccountESMessage
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
          case AccountESMessage.Join(userId, replier) =>
            actor
              .?(AccountEventSourced.Join(userId))
              .flatMap { tryMembers =>
                replier.reply(tryMembers)
              }
          case AccountESMessage.Leave(userId) =>
            actor
              .?(AccountEventSourced.Leave(userId))
              .unit
        }
      }
}
