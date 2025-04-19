package qmf.poc.service.http.handlers.queries

import qmf.poc.service.http.handlers.params.{NoParameterException, param, paramAgentId}
import qmf.poc.service.messages.{OmRequestRunObject, OutgoingMessageError, OutgoingMessageHandler, OutgoingMessageIdGenerator}
import zio.{Cause, ZIO}
import zio.http.{Request, Response, Status}

def run: Request => ZIO[OutgoingMessageHandler & OutgoingMessageIdGenerator, Nothing, Response] = (request: Request) =>
  (for {
    agentId <- paramAgentId(request)
    owner <- param(request, "owner")
    name <- param(request, "name")
    limitS = request.url.queryParams("limit").headOption.getOrElse("-1")
    limit <- ZIO.attempt(limitS.toInt).orElseFail(OutgoingMessageError("Invalid limit parameter", None))
    id <- ZIO.serviceWithZIO[OutgoingMessageIdGenerator](_.nextId)
    promise <- ZIO.serviceWithZIO[OutgoingMessageHandler](_.put(agentId, OmRequestRunObject(id, owner, name, limit)))
    result <- promise.await
    response <- ZIO.succeed(Response.text(result.body))
  } yield response)
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
