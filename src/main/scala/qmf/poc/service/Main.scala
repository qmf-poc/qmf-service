package qmf.poc.service

import qmf.poc.service.http.handlers.ws.Broker
import qmf.poc.service.http.server
import zio.*
import zio.http.Server

object Main extends ZIOAppDefault:
  override def run: ZIO[Environment & ZIOAppArgs & Scope, Any, Any] =
    val program = for
      httpStarted <- server
      _ <- httpStarted.await // wait until the server started
      _ <- ctrlC
      _ <- ZIO.never // TODO: should be clean up
    yield ()

    program.provide(
      ZLayer.succeed(Server.Config.default.port(8080)), // use predefined configuration
      Broker.layer
    )
