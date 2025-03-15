package qmf.poc.service.http.handlers.rest

import qmf.poc.service.agent.{Broker, OutgoingMessageIdGenerator, RequestSnapshot}
import zio.ZIO
import zio.http.{Request, Response}

def snapshot: Request => ZIO[Broker & OutgoingMessageIdGenerator, Nothing, Response] = (request: Request) =>
  for {
    broker <- ZIO.service[Broker]
    id <- ZIO.serviceWithZIO[OutgoingMessageIdGenerator](_.nextId)
    promise <- broker.put(RequestSnapshot.default(id))
    _ <- promise.await
    response <- ZIO.succeed(Response.text(s"synced"))
  } yield response

/*
  ZIO
    .serviceWithZIO[Broker] { broker =>
      broker.put(RequestSnapshot.default).as(Response.text("Refresh requested"))
    }
    .catchAll(th => ZIO.logError(th.getMessage).as(Response.text(th.getMessage).status(Status.BadRequest)))
 */
