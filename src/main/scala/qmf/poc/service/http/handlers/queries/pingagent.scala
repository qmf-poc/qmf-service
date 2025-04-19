package qmf.poc.service.http.handlers.queries

import qmf.poc.service.http.handlers.params.{NoParameterException, paramAgentId}
import qmf.poc.service.messages.{OmPing, OutgoingMessageError, OutgoingMessageHandler, OutgoingMessageIdGenerator}
import zio.{Cause, ZIO}
import zio.http.{Request, Response, Status}

def pingAgent: Request => ZIO[OutgoingMessageHandler & OutgoingMessageIdGenerator, Nothing, Response] = (request: Request) =>
  val payload = s"ping-agent ${System.currentTimeMillis()}"
  (for {
    agentId <- paramAgentId(request)
    id <- ZIO.serviceWithZIO[OutgoingMessageIdGenerator](_.nextId)
    promise <- ZIO.serviceWithZIO[OutgoingMessageHandler] { _.put(agentId, OmPing(id, payload)) }
    pong <- promise.await
  } yield Response.text(s"pong: $pong"))
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
