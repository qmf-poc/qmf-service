package qmf.poc.service.http.handlers.rest

import qmf.poc.service.agent.{AgentError, Broker, OutgoingMessageIdGenerator, RequestRunObject}
import zio.ZIO
import zio.http.{Request, Response, Status}

def run: Request => ZIO[Broker & OutgoingMessageIdGenerator, Nothing, Response] = (request: Request) =>
  val owner = request.url.queryParams("owner").headOption.getOrElse("null")
  val name = request.url.queryParams("name").headOption.getOrElse("null")
  val limitS = request.url.queryParams("limit").headOption.getOrElse("-1")
  (for {
    limit <- ZIO.attempt(limitS.toInt).orElseFail(AgentError("Invalid limit parameter", None))
    broker <- ZIO.service[Broker]
    id <- ZIO.serviceWithZIO[OutgoingMessageIdGenerator](_.nextId)
    promise <- broker.put(RequestRunObject(id, owner, name, limit))
    result <- promise.await
    response <- ZIO.succeed(Response.text(result.body))
  } yield response).catchAll(error =>
    ZIO
      .logError(error.message)
      .as(
        Response
          .text(error.message)
          .status(Status.BadRequest)
      )
  )

/*
  ZIO
    .serviceWithZIO[Broker] { broker =>
      broker.put(RequestRunObject(owner, name)).as(Response.text("Run requested"))
    }
    .catchAll(th => ZIO.logError(th.getMessage).as(Response.text(th.getMessage).status(Status.BadRequest)))
 */
