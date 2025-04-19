package qmf.poc.service.http.handlers.queries

import qmf.poc.service.http.handlers.params.{NoParameterException, paramAgentId}
import qmf.poc.service.messages.{OmRequestSnapshot, OutgoingMessageError, OutgoingMessageHandler, OutgoingMessageIdGenerator}
import zio.{Cause, ZIO}
import zio.http.{Request, Response, Status}

def snapshot: Request => ZIO[OutgoingMessageHandler & OutgoingMessageIdGenerator, Nothing, Response] = (request: Request) =>
  (for {
    agentId <- paramAgentId(request)
    id <- ZIO.serviceWithZIO[OutgoingMessageIdGenerator](_.nextId)
    promise <- ZIO.serviceWithZIO[OutgoingMessageHandler](_.put(agentId, OmRequestSnapshot.default(id)))
    _ <- promise.await
  } yield Response.text("synced"))
    .catchAll {
      case e: NoParameterException =>
        ZIO
          .logErrorCause(e.message, Cause.fail(e))
          .as(
            Response.text(e.message).status(Status.BadRequest)
          )
      case e: OutgoingMessageError =>
        ZIO
          .logErrorCause(e.message, Cause.fail(e))
          .as(
            Response.text(e.message).status(Status.InternalServerError)
          )
    }

/*
  ZIO
    .serviceWithZIO[Broker] { broker =>
      broker.put(RequestSnapshot.default).as(Response.text("Refresh requested"))
    }
    .catchAll(th => ZIO.logError(th.getMessage).as(Response.text(th.getMessage).status(Status.BadRequest)))
    // handler <- ZIO.service[OutgoingMessageHandler]
    // promise <- broker.put(OmRequestSnapshot.default(id))
    response <- promise.await.fold(
      err => { Response.error(Status.InternalServerError, err.message) },
      _ => { Response.text(s"synced") }
    )
 */
