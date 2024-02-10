package accountmanagement.actor

import accountmanagement.app.AccountManagementProtocol.AccountInfo
import zio.actors.Context
import zio.actors.persistence.{ Command, EventSourcedStateful, PersistenceId }
import zio.{ UIO, ZIO }

import scala.util.{ Failure, Success, Try }
import zio.actors.Supervisor
import zio.actors.ActorRef
import infra.Layers.ActorSystemZ

object AccountEventSourced {
  case class Transaction(amount: BigDecimal, description: String)

  sealed trait AccountMessage[+_]
  case class ApplyTransaction(tx: Transaction) extends AccountMessage[Try[AccountInfo]]
  case object Get                              extends AccountMessage[AccountState]

  sealed trait AccountEvent
  case class TransactionApplied(tx: Transaction) extends AccountEvent
  case class AccountState(txs: List[Transaction], balance: BigDecimal, userDetails: String = "") {
    def isValid(tx: Transaction): Boolean = balance + tx.amount > 0.0
  }
  object AccountState {
    def empty: AccountState = AccountState(List.empty[Transaction], BigDecimal(0.0))
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
          case ApplyTransaction(tx) =>
            if (state.isValid(tx)) {
              ZIO.succeed(
                (
                  Command.persist(TransactionApplied(tx)),
                  st => Success(AccountInfo(persistenceId, st.balance, st.userDetails)).asInstanceOf[A]
                )
              )
            } else {
              ZIO.succeed((Command.ignore, _ => Failure(new Exception("Insufficient Funds!")).asInstanceOf[A]))
            }
          case Get => ZIO.succeed((Command.ignore, _ => state.asInstanceOf[A]))
        }

      override def sourceEvent(state: AccountState, event: AccountEvent): AccountState =
        event match {
          case TransactionApplied(tx) =>
            state.copy(
              txs = tx :: state.txs,
              balance = state.balance + tx.amount
            )
        }
    }

  // This actorRef function will either hold a reference to our actor for a given accountId or create one
  // This means that each account will only have a single threaded handling of its own state, but we can have as many
  // accounts running in parallel as we want
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
