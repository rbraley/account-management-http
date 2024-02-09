package accountmanagement.app

import com.devsisters.shardcake.{
  Config,
  GrpcConfig,
  GrpcPods,
  GrpcShardingService,
  KryoSerialization,
  RedisConfig,
  ShardManagerClient,
  Sharding,
  StorageRedis
}
import infra.Layers
import sttp.client3.UriContext
import zio.{ Scope, System, ZLayer }
import zio.http.{ Handler, Routes, Server }

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

  val getAccountRoute =
    getAccount.implement {
      Handler.fromFunctionZIO(getAccountHandler)
    }

  val createTransactionRoute =
    createTransaction.implement {
      Handler.fromFunctionZIO { tx =>
        createTransactionHandler(tx).debug("createTransaction: ").orDie
      }
    }

  //  val getUserPostsRoute =
  //    getUserPosts.implement {
  //      Handler.fromFunction { case (userId, postId, name) =>
  //        List(Post(userId, postId, name))
  //      }
  //    }
  //
  //  val createUserRoute =
  //    createUser.implement {
  //      Handler.fromFunction { user =>
  //        user.name
  //      }
  //    }

  val routes = Routes(getAccountRoute, createTransactionRoute) // , getUserPostsRoute, createUserRoute)

  val run = Server
    .serve(routes.toHttpApp)
    .provide(
      config,
      Scope.default,
      Server.defaultWithPort(8081),
      AccountManager.live,
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
