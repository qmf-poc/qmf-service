package qmf.poc.service

import qmf.poc.service.http.handlers.ws.{Broker, BrokerLive, OutgoingMessage}
import qmf.poc.service.http.server
import qmf.poc.service.repository.{LuceneRepository, Repository}
import zio.*
import zio.Console.printLine
import zio.http.{Server, SocketDecoder, WebSocketConfig}
import zio.http.netty.NettyConfig

object Main extends ZIOAppDefault:

  override val bootstrap: ZLayer[Any, Nothing, Unit] =
    Runtime.removeDefaultLoggers ++
      Runtime
        .addLogger(
          ZLogger.default
            .map(println(_))
            .filterLogLevel(_ >= LogLevel.Debug)
        )

  override def run: ZIO[Environment & ZIOAppArgs & Scope, Any, Any] =
    val repositoryLayer: ULayer[Repository] = LuceneRepository.layer
    val brokerQueueLayer: ULayer[Queue[OutgoingMessage]] = ZLayer(Queue.sliding[OutgoingMessage](100))
    val brokerLayer: ULayer[Broker] = (repositoryLayer ++ brokerQueueLayer) >>> BrokerLive.layer

    val httpConfigLayer: ULayer[Server.Config] = ZLayer.succeed(
      Server.Config.default
        .port(8081)
        .webSocketConfig(
          WebSocketConfig.default
            .decoderConfig(SocketDecoder.default.maxFramePayloadLength(Int.MaxValue))
        )
    )
    val nettyConfigLayer: ULayer[NettyConfig] = ZLayer.succeed(NettyConfig.default)
    val serverLayer: TaskLayer[Server] = (nettyConfigLayer ++ httpConfigLayer) >>> Server.live

    val program = for
      (httpStarted, shutdownSignal) <- server
      _ <- httpStarted.await
      _ <- printLine("Server started")
      _ <- ctrlC
      _ <- ZIO.never // TODO: should be clean up
    yield ()

    program.provideSome(repositoryLayer ++ brokerLayer ++ serverLayer)
