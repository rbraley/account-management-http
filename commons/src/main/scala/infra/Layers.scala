package infra

import com.devsisters.shardcake.{Messenger, Sharding}
import com.devsisters.shardcake.StorageRedis.Redis
import com.devsisters.shardcake.interfaces.Serialization
import dev.profunktor.redis4cats.{Redis, RedisCommands}
import dev.profunktor.redis4cats.connection.RedisClient
import dev.profunktor.redis4cats.data.RedisCodec
import dev.profunktor.redis4cats.effect.Log
import dev.profunktor.redis4cats.pubsub.PubSub
import zio.actors.ActorSystem
import zio.interop.catz._
import zio.{Scope, Task, ZEnvironment, ZIO, ZLayer}

import java.io.File
import zio.actors.ActorSystemUtils

import scala.util.Try

object Layers {
  val redis: ZLayer[Any, Throwable, Redis] =
    ZLayer.scopedEnvironment {
      implicit val runtime: zio.Runtime[Any] = zio.Runtime.default
      implicit val logger: Log[Task] = new Log[Task] {
        override def debug(msg: => String): Task[Unit] = ZIO.logDebug(msg)
        override def error(msg: => String): Task[Unit] = ZIO.logError(msg)
        override def info(msg: => String): Task[Unit]  = ZIO.logInfo(msg)
      }

      (for {
        client   <- RedisClient[Task].from("redis://redis")
        commands <- Redis[Task].fromClient(client, RedisCodec.Utf8)
        pubSub   <- PubSub.mkPubSubConnection[Task, String, String](client, RedisCodec.Utf8)
      } yield ZEnvironment(commands, pubSub)).toScopedZIO
    }

  case class ActorSystemZ(
      name: String,
      system: ActorSystem
  ) {
    val basePath = s"zio://$name@0.0.0.0:0000/"
  }

  def applicationConf: Option[File] = {
    Try(new File(getClass.getResource("application.conf").getFile))
      .recover(_ => new File("/opt/docker/application.conf"))
      .toOption
  }



  def actorSystem(name: String): ZLayer[Any, Throwable, ActorSystemZ] =
    ZLayer {
      ActorSystem(name, applicationConf)
        .map { system => ActorSystemZ(name, system) }
    }
}
