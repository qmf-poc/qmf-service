package qmf.poc.service.http

import qmf.poc.service.agent.AgentsRegistry
import qmf.poc.service.messages.MessageJson.given
import qmf.poc.service.http.handlers.queries.*
import qmf.poc.service.http.handlers.ws.agentWebsocketApp
import qmf.poc.service.messages.{IncomingMessageHandler, OutgoingMessageHandler, OutgoingMessageIdGenerator, OutgoingMessagesStorage}
import qmf.poc.service.qmfstorage.QmfObjectsStorage
import zio.http.*
import zio.http.Header.AccessControlAllowOrigin
import zio.http.Method.GET
import zio.http.Middleware.{CorsConfig, cors}
import zio.{Console, Promise, ZIO}

private val config: CorsConfig =
  CorsConfig(
    allowedOrigin = _ => AccessControlAllowOrigin.parse("*").toOption
  )

private def log: HandlerAspect[Any, Unit] =
  HandlerAspect.interceptHandlerStateful(Handler.fromFunctionZIO[Request] { request =>
    zio.Clock.instant.map(now => ((now, request), (request, ())))
  })(Handler.fromFunctionZIO[((java.time.Instant, Request), Response)] { case ((start, request), response) =>
    zio.Clock.instant.flatMap { end =>
      val duration = java.time.Duration.between(start, end)

      Console
        .printLine(
          s"${request.remoteAddress.map(_.toString).getOrElse("unknown")} ${response.status.code} ${request.method} ${request.url.encode} ${duration.toMillis}ms"
        )
        .orDie
        .as(response)
    }
  })

def routes: Routes[
  AgentsRegistry & OutgoingMessagesStorage & IncomingMessageHandler & OutgoingMessageIdGenerator & OutgoingMessageHandler &
    QmfObjectsStorage,
  Nothing
] =
  Routes(
    GET / "ping" -> handler(ping),
    GET / "agent-ping" -> handler(pingAgent),
    GET / "ping-agent" -> handler(pingAgent),
    GET / "catalog" -> handler(snapshot),
    GET / "agent" -> handler(agentWebsocketApp.toResponse),
    GET / "retrieve" -> handler(query),
    GET / "query" -> handler(query),
    GET / "get" -> handler(get),
    GET / "run" -> handler(run)
  ) @@ cors(config) @@ log

def qmfHttpServer: ZIO[
  AgentsRegistry & IncomingMessageHandler & OutgoingMessagesStorage & OutgoingMessageIdGenerator & OutgoingMessageHandler &
    QmfObjectsStorage & Server,
  Throwable,
  (Promise[Nothing, Unit], Promise[Nothing, Unit])
] =
  for
    httpStarted <- Promise.make[Nothing, Unit]
    shutdownSignal <- Promise.make[Nothing, Unit]
    repository <- ZIO.service[QmfObjectsStorage]
    port <- ZIO.serviceWithZIO[Server](_.install(routes))
    _ <- httpStarted.succeed(())
    _ <- shutdownSignal.await.forkDaemon
  yield (httpStarted, shutdownSignal)
