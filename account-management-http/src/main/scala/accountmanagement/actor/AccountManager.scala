package accountmanagement.actor

import accountmanagement.behavior.AccountESBehavior
import accountmanagement.behavior.AccountESBehavior.{AccountES, behavior}
import com.devsisters.shardcake.interfaces.Serialization
import com.devsisters.shardcake.{Messenger, Sharding}
import dev.profunktor.redis4cats.RedisCommands
import infra.Layers.ActorSystemZ
import zio.{Scope, Task, ZLayer}

object AccountManager {
  type ActorEnv = Sharding with ActorSystemZ with Scope with Serialization with RedisCommands[Task, String, String]
  type AccountManager = Messenger[AccountESBehavior.AccountESMessage]
  val layer: ZLayer[ActorEnv, Throwable, AccountManager] = ZLayer {
    for {
      _              <- Sharding.registerEntity(AccountES, behavior)
      _              <- Sharding.registerScoped
      accountManager <- Sharding.messenger(AccountES)
    } yield accountManager
  }
}
