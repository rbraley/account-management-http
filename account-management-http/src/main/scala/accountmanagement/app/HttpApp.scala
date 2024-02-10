package accountmanagement.app

import accountmanagement.actor.AccountManager
import accountmanagement.app.Routes.{ createTransactionRoute, getAccountRoute, getTransactionHistoryRoute }
import com.devsisters.shardcake._
import infra.Layers
import sttp.client3.UriContext
import zio.http.{ Routes, Server }
import zio.{ Scope, System, ZLayer }

/*
 * This is the main entrypoint into the application. It serves an Http Server with 3 routes
 *
 * GET /account/{account_id}
 * POST /transaction
 * GET /transaction/history/{account_id}
 */
object HttpApp extends zio.ZIOAppDefault with Endpoints with Handlers {
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

  val routes = Routes(getAccountRoute, createTransactionRoute, getTransactionHistoryRoute)

  val run = Server
    .serve(routes.toHttpApp)
    .provide(
      config,
      Scope.default,
      Server.defaultWithPort(8081),
      AccountManager.layer,
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
