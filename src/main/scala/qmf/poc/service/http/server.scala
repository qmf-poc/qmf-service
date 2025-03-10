package qmf.poc.service.http

import qmf.poc.service.http.handlers.rest.{ping, query, get}
import qmf.poc.service.http.handlers.ws.{Broker, RequestSnapshot, agentWebsocketApp}
import qmf.poc.service.repository.Repository
import zio.http.Method.GET
import zio.http.*
import zio.http.Header.{AccessControlAllowOrigin, Origin}
import zio.http.Middleware.{CorsConfig, cors}
import zio.{Promise, ZIO}

private val config: CorsConfig =
  CorsConfig(
    allowedOrigin = (_ => AccessControlAllowOrigin.parse("*").toOption)
  )

def routes(broker: Broker, repository: Repository) =
  Routes(
    GET / "ping" -> handler(ping),
    GET / "catalog" ->
      handler((_: Request) => broker.put(RequestSnapshot.default).ignore.as(Response.text("Refresh requested"))),
    GET / "agent" -> handler(agentWebsocketApp.toResponse),
    GET / "retrieve" -> handler(query(repository)),
    GET / "query" -> handler(query(repository)),
    GET / "get" -> handler(get(repository)),
    GET / "object" -> handler(query(repository))
  ) @@ cors(config) @@ Middleware.debug

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
