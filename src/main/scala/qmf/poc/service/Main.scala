package qmf.poc.service

import qmf.poc.service.http.handlers.ws.Broker
import qmf.poc.service.http.server
import zio.*
import zio.Console.printLine
import zio.http.Server

object Main extends ZIOAppDefault:
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

    program.provideSome(
      ZLayer.succeed(Server.Config.default.port(8080)), // use predefined configuration
      Broker.layer,
      Server.live,
    )
