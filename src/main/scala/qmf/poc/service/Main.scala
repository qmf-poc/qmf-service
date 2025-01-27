package qmf.poc.service

import qmf.poc.service.http.handlers.ws.{Broker, BrokerLive, OutgoingMessage}
import qmf.poc.service.http.server
import qmf.poc.service.repository.{LuceneRepository, Repository}
import zio.*
import zio.Console.printLine
import zio.http.{Driver, Server}

object Main extends ZIOAppDefault:
  private val repositoryLayer: ULayer[Repository] = LuceneRepository.layer
  private val brokerQueueLayer: ULayer[Queue[OutgoingMessage]] = ZLayer(Queue.sliding[OutgoingMessage](100))
  private val brokerLayer: ULayer[Broker] = (repositoryLayer ++ brokerQueueLayer) >>> BrokerLive.layer

  private val httpConfigLayer: ULayer[Server.Config] = ZLayer.succeed(Server.Config.default.port(8080))
  private val serverLayer: TaskLayer[Server] = httpConfigLayer >>> Server.live

  override val bootstrap: ZLayer[Any, Nothing, Unit] =
    Runtime.removeDefaultLoggers ++
      Runtime
        .addLogger(ZLogger.default.map(println(_))
          .filterLogLevel(_ >= LogLevel.Debug))

  override def run: ZIO[Environment & ZIOAppArgs & Scope, Any, Any] =
    val program = for
      (httpStarted, shutdownSignal) <- server
      _ <- httpStarted.await
      _ <- printLine("Server started")
      _ <- ctrlC
      _ <- ZIO.never // TODO: should be clean up
    yield ()

    program.provideSome(repositoryLayer ++ brokerLayer ++ serverLayer)