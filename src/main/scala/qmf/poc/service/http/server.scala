package qmf.poc.service.http

import qmf.poc.service.http.handlers.rest.{ping, retrieve}
import qmf.poc.service.http.handlers.ws.{Broker, RequestSnapshot, agentWebsocketApp}
import qmf.poc.service.repository.Repository
import zio.http.Method.GET
import zio.http.*
import zio.{Promise, ZIO}


def routes(broker: Broker, repository: Repository) = Routes(
  GET / "ping" -> handler(ping),
  GET / "catalog" -> handler((_: Request) => broker.put(RequestSnapshot("db2inst1", "password")).ignore.as(Response.text("Refresh requested"))),
  GET / "agent" -> handler(agentWebsocketApp.toResponse),
  GET / "retrieve" -> handler(retrieve(repository))
)

def server: ZIO[Broker & Server & Repository, Throwable, (Promise[Nothing, Unit], Promise[Nothing, Unit])] =
  for
    httpStarted <- Promise.make[Nothing, Unit]
    shutdownSignal <- Promise.make[Nothing, Unit]
    broker <- ZIO.service[Broker]
    repository <- ZIO.service[Repository]
    port <- ZIO.serviceWithZIO[Server](_.install(routes(broker, repository)))
    _ <- httpStarted.succeed(())
    _ <- shutdownSignal.await.forkDaemon
  yield (httpStarted, shutdownSignal)
