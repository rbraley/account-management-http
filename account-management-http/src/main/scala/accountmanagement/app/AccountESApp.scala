package accountmanagement.app

import com.devsisters.shardcake._
import com.devsisters.shardcake.interfaces.Serialization
import dev.profunktor.redis4cats.RedisCommands
import accountmanagement.behavior.AccountESBehavior.AccountESMessage.Join
import accountmanagement.behavior.AccountESBehavior.{ AccountES, behavior }
import infra.Layers
import infra.Layers.ActorSystemZ
import sttp.client3.UriContext
import zio.{ Random, Scope, System, Task, ZIO, ZIOAppDefault, ZLayer }

object AccountESApp extends ZIOAppDefault {
  private val defaultConfig = Config.default.copy(
    shardManagerUri = uri"http://shard-manager:8080/api/graphql",
    selfHost = "account-management-http"
  )
  val config: ZLayer[Any, SecurityException, Config] =
    ZLayer(
      System
        .env("port")
        .map(
          _.flatMap(_.toIntOption).fold(defaultConfig)(port => defaultConfig.copy(shardingPort = port))
        )
    )

  val program: ZIO[
    Sharding with ActorSystemZ with Scope with Serialization with RedisCommands[Task, String, String],
    Throwable,
    Unit
  ] =
    for {
      _              <- Sharding.registerEntity(AccountES, behavior)
      _              <- Sharding.registerScoped
      accountManager <- Sharding.messenger(AccountES)
      user1          <- Random.nextUUID.map(_.toString)
      user2          <- Random.nextUUID.map(_.toString)
      user3          <- Random.nextUUID.map(_.toString)
      _              <- accountManager.send("account1")(Join(user1, _)).debug
      _              <- accountManager.send("account1")(Join(user2, _)).debug
      _              <- accountManager.send("account1")(Join(user3, _)).debug
      _              <- ZIO.never
    } yield ()

  def run: Task[Unit] =
    ZIO
      .scoped(program)
      .provide(
        config,
        ZLayer.succeed(GrpcConfig.default),
        ZLayer.succeed(RedisConfig.default),
        Layers.redis,
        Layers.actorSystem("AccountSystem"),
        StorageRedis.live,
        KryoSerialization.live,
        ShardManagerClient.liveWithSttp,
        GrpcPods.live,
        Sharding.live,
        GrpcShardingService.live
      )
}
