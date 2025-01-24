package qmf.poc.service.http

import qmf.poc.service.http.handlers.rest.ping
import qmf.poc.service.http.handlers.ws.{Broker, agent, echo}
import zio.Console.printLine
import zio.{Promise, ZIO}
import zio.http.Method.GET
import zio.http.{Routes, Server, handler}


package object http

val routes = Routes(
  GET / "ping" -> handler(ping),
  GET / "echo" -> handler(echo.toResponse),
  GET / "agent" -> handler(agent.toResponse),
)

def server: ZIO[Server.Config & Broker, Throwable, Promise[Nothing, Unit]] =
  (for
    httpStarted <- Promise.make[Nothing, Unit]
    _ <- httpStarted.succeed(())
    _ <- Server.serve(routes)
    // does not work for some reason
    //port <- Server.install(routes)
    //_ <- (httpStarted.succeed(()) *> ZIO.logInfo(s"Server started $port") *> ZIO.never).fork
  yield httpStarted).provideSomeLayer(Server.live)

