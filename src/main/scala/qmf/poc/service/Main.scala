package qmf.poc.service

import qmf.poc.service.http.routes
import zio.*
import zio.Console.printLine
import zio.http.Server

object Main extends ZIOAppDefault:
  private def putStrLn(s: String)(implicit trace: Trace)  = printLine(s).orElse(ZIO.unit).unit

  override def run: ZIO[Environment & ZIOAppArgs & Scope, Any, Any] =
    for
      httpStarted <- Promise.make[Nothing, Unit] // i'll wait this promise to print out the HTTP server started message
      fiber <- Server
        .install(routes) // now the server started listening
        .zipRight(httpStarted.succeed(())) // signal the promise completed
        .zipRight(ZIO.never) // wait forever
        .provide(
          Server.live, // provide the server implementation from zio-http
          ZLayer.succeed(Server.Config.default.port(8080)), // use predefined configuration
        )
        .fork // launch the server in its own fiber
      _ <- httpStarted.await // wait until the server started
      _ <- putStrLn("Server started") // print out the server started message
      _ <- ZIO.never // TODO: should be clean up
    yield ()
