package qmf.poc.service.http

import qmf.poc.service.http.handlers.rest.ping
import qmf.poc.service.http.handlers.ws.{Broker, RequestSnapshot, agentWebsocketApp}
import zio.http.Method.GET
import zio.http.*
import zio.{Promise, ZIO}


package object http

def routes(broker: Broker) = Routes(
  GET / "ping" -> handler(ping),
  GET / "catalog" -> handler((_: Request) => broker.put(RequestSnapshot("db2inst1", "password")).ignore.as(Response.text("Refresh requested"))),
  GET / "agent" -> handler(agentWebsocketApp.toResponse),
)

def server: ZIO[Server.Config & Broker & Server /* & Scope*/ , Throwable, (Promise[Nothing, Unit], Promise[Nothing, Unit])] =
  for
    httpStarted <- Promise.make[Nothing, Unit]
    shutdownSignal <- Promise.make[Nothing, Unit]
    broker <- ZIO.service[Broker]
    port <- ZIO.serviceWithZIO[Server](_.install(routes(broker)))
    _ <- httpStarted.succeed(())
    _ <- shutdownSignal.await.forkDaemon
  yield (httpStarted, shutdownSignal)
//.provideSomeLayer(Server.live)

