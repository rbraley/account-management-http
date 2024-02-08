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
      user1 <- Random.nextUUID.map(_.toString)
      user2 <- Random.nextUUID.map(_.toString)
      persistenceId1 = "account1"
      account1 <- actorSystem.make(persistenceId1, Supervisor.none, AccountState.empty, handler(persistenceId1))
      _        <- account1 ? Join(user1)
      _        <- account1 ? Join(user2)
      members1 <- account1 ? Get
      _        <- Console.printLine(s"members1: $members1")
      _        <- account1.stop
      // Scenario 2
      persistenceId2 = "account2"
      account2 <- actorSystem.make(persistenceId2, Supervisor.none, AccountState.empty, handler(persistenceId2))
      _        <- account2 ? Join(user1)
      _        <- account2 ? Join(user2)
      members2 <- account2 ? Get
      _        <- Console.printLine(s"members2: $members2")
      _        <- account2.stop
      // Scenario 3
      persistenceId1 = "account1"
      account1B <- actorSystem.make(persistenceId1, Supervisor.none, AccountState.empty, handler(persistenceId1))
      user3     <- Random.nextUUID.map(_.toString)
      user4     <- Random.nextUUID.map(_.toString)
      _         <- account1B ? Join(user3)
      _         <- account1B ? Join(user4)
      members1  <- account1B ? Get
      _         <- account1B.stop
      _         <- actorSystem.shutdown
    } yield members1.members == (members2.members ++ Set(user3, user4))

    val runtime = zio.Runtime.default
    Unsafe.unsafe { implicit unsafe =>
      runtime.unsafe
        .runToFuture(io)
        .future
        .map(_ should be(true))
    }
  }
}
