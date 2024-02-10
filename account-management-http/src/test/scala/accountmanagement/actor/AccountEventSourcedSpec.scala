package accountmanagement.actor

import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import zio.actors.{ ActorSystem, Supervisor }
import zio.{ Unsafe, _ }

import java.io.File

class AccountEventSourcedSpec extends AsyncWordSpec with Matchers with BeforeAndAfterAll {

  "Persistent actors should be recover state after complete system shutdown" in {
    import accountmanagement.actor.AccountEventSourced._
    val io = for {
      actorSystem <- ActorSystem(
        "AccountSystem",
        Some(new File("account-management-http/src/main/resources/application.conf"))
      )
      // Scenario 1
      persistenceId1 = "account1"
      tx1            = Transaction(10, "groceries")
      account1 <- actorSystem.make(persistenceId1, Supervisor.none, AccountState.empty, handler(persistenceId1))
      _        <- account1 ? ApplyTransaction(tx1)
      _        <- account1 ? ApplyTransaction(tx1)
      state1   <- account1 ? Get
      _        <- Console.printLine(s"state1: $state1")
      _        <- account1.stop
      // Scenario 2
      persistenceId2 = "account2"
      account2 <- actorSystem.make(persistenceId2, Supervisor.none, AccountState.empty, handler(persistenceId2))
      _        <- account2 ? ApplyTransaction(tx1)
      _        <- account2 ? ApplyTransaction(tx1)
      state2   <- account2 ? Get
      _        <- Console.printLine(s"state2: $state2")
      _        <- account2.stop
      // Scenario 3
      persistenceId1 = "account1"
      account1B <- actorSystem.make(persistenceId1, Supervisor.none, AccountState.empty, handler(persistenceId1))
      _         <- account1B ? ApplyTransaction(tx1)
      _         <- account1B ? ApplyTransaction(tx1)
      state1B   <- account1B ? Get
      _         <- account1B.stop
      _         <- actorSystem.shutdown
    } yield state1B.balance == (state2.balance + state1.balance)

    val runtime = zio.Runtime.default
    Unsafe.unsafe { implicit unsafe =>
      runtime.unsafe
        .runToFuture(io)
        .future
        .map(_ should be(true))
    }
  }
}
