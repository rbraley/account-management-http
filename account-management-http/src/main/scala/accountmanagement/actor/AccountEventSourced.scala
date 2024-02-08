package accountmanagement.actor

import zio.actors.Context
import zio.actors.persistence.{ Command, EventSourcedStateful, PersistenceId }
import zio.{ UIO, ZIO }

import scala.util.{ Failure, Success, Try }
import zio.actors.Supervisor
import zio.actors.ActorRef
import infra.Layers.ActorSystemZ

object AccountEventSourced {
  sealed trait AccountMessage[+_]
  case class Join(userId: String)  extends AccountMessage[Try[Set[String]]]
  case class Leave(userId: String) extends AccountMessage[Unit]
  case object Get                  extends AccountMessage[AccountState]

  sealed trait AccountEvent
  case class JoinedEvent(userId: String) extends AccountEvent
  case class LeftEvent(userId: String)   extends AccountEvent

  case class AccountState(members: Set[String])
  object AccountState {
    def empty: AccountState = AccountState(members = Set.empty)
  }

  def handler(persistenceId: String): EventSourcedStateful[Any, AccountState, AccountMessage, AccountEvent] =
    new EventSourcedStateful[Any, AccountState, AccountMessage, AccountEvent](
      PersistenceId(persistenceId)
    ) {
      override def receive[A](
          state: AccountState,
          msg: AccountMessage[A],
          context: Context
      ): UIO[(Command[AccountEvent], AccountState => A)] =
        msg match {
          case Join(userId) =>
            if (state.members.size >= 5) {
              ZIO.succeed((Command.ignore, _ => Failure(new Exception("Account is already full!")).asInstanceOf[A]))
            } else {
              ZIO.succeed((Command.persist(JoinedEvent(userId)), st => Success(st.members).asInstanceOf[A]))
            }
          case Leave(userId) => ZIO.succeed((Command.persist(LeftEvent(userId)), _ => ().asInstanceOf[A]))
          case Get           => ZIO.succeed((Command.ignore, _ => state.asInstanceOf[A]))
        }

      override def sourceEvent(state: AccountState, event: AccountEvent): AccountState =
        event match {
          case JoinedEvent(userId) =>
            state.copy(
              members = state.members + userId
            )
          case LeftEvent(userId) =>
            state.copy(
              members = state.members - userId
            )
        }
    }

  def actorRef(
      entityId: String
  ): ZIO[ActorSystemZ, Throwable, ActorRef[AccountEventSourced.AccountMessage]] =
    ZIO.serviceWithZIO[ActorSystemZ] { actorSystemZ =>
      actorSystemZ.system
        .select[AccountEventSourced.AccountMessage](actorSystemZ.basePath + entityId)
        .orElse(
          actorSystemZ.system
            .make(
              entityId,
              Supervisor.none,
              AccountState.empty,
              AccountEventSourced.handler(entityId)
            )
        )
    }
}
