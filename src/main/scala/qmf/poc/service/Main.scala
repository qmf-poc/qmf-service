package qmf.poc.service

import qmf.poc.service.agent.{AgentRegistryLive, AgentsRegistry}
import qmf.poc.service.http.qmfHttpServer
import qmf.poc.service.messages.{
  IncomingMessage,
  IncomingMessageHandler,
  IncomingMessageHandlerLive,
  OutgoingMessage,
  OutgoingMessageError,
  OutgoingMessageHandler,
  OutgoingMessageHandlerLive,
  OutgoingMessageIdGenerator,
  OutgoingMessagesStorage
}
import qmf.poc.service.qmfstorage.QmfObjectsStorage
import qmf.poc.service.qmfstorage.lucene.QmfObjectsStorageLucene
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
    // ZIO Http Server
    val layerHttpConfig: ULayer[Server.Config] = ZLayer.succeed(
      Server.Config.default
        .port(8081)
        .webSocketConfig(
          WebSocketConfig.default
            .decoderConfig(SocketDecoder.default.maxFramePayloadLength(Int.MaxValue))
        )
    )
    val layerNettyConfig: ULayer[NettyConfig] = ZLayer.succeed(NettyConfig.default)
    val layerZIOServer: TaskLayer[Server] = (layerNettyConfig ++ layerHttpConfig) >>> Server.live
    // QMF Objects Storage
    val layerQmfObjectsStorage: ULayer[QmfObjectsStorage] = QmfObjectsStorageLucene.layer
    // Agents registry
    val agentsRegistryLayer: ULayer[AgentsRegistry] = AgentRegistryLive.layer
    // JsonRPC
    val pendingRequestsPromises: ULayer[Ref[Map[Int, Promise[OutgoingMessageError, IncomingMessage]]]] =
      ZLayer(Ref.make(Map.empty[Int, Promise[OutgoingMessageError, IncomingMessage]]))
    // Outgoing
    val outgoingMessageIdGenerator: ULayer[OutgoingMessageIdGenerator] = OutgoingMessageIdGenerator.layer
    val outgoingMessageHandler: ULayer[OutgoingMessageHandler] = pendingRequestsPromises >>> OutgoingMessageHandlerLive.live
    val outgoingAgentTransport = outgoingMessageHandler ++ outgoingMessageIdGenerator
    // Incoming
    val incomingMessageHandlerLayer = (layerQmfObjectsStorage ++ pendingRequestsPromises) >>> IncomingMessageHandlerLive.layer
    val outgoingMessagesStorage: ULayer[OutgoingMessagesStorage] = ZLayer(OutgoingMessagesStorage.live)
    val incomingAgentTransport = incomingMessageHandlerLayer ++ outgoingMessagesStorage

    val program: ZIO[
      AgentsRegistry & IncomingMessageHandler & OutgoingMessagesStorage & OutgoingMessageIdGenerator & OutgoingMessageHandler &
        QmfObjectsStorage & Server,
      Throwable,
      Unit
    ] = for {
      (httpStarted, shutdownSignal) <- qmfHttpServer
      _ <- httpStarted.await
      _ <- printLine("Server started 8081")
      // _ <- ctrlC
      _ <- ZIO.never // TODO: should be clean up
    } yield ()

    program.provideSome(
      layerZIOServer ++ layerQmfObjectsStorage ++ agentsRegistryLayer ++ outgoingAgentTransport ++ incomingAgentTransport
    )
