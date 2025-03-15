package qmf.poc.service.http.handlers.rest

import qmf.poc.service.agent.{Broker, RequestRunObject}
import zio.ZIO
import zio.http.{Request, Response, Status}

def run: Request => ZIO[Broker, Nothing, Response] = (request: Request) =>
  val owner = request.url.queryParams("owner").headOption.getOrElse("null")
  val name = request.url.queryParams("name").headOption.getOrElse("null")
  ZIO
    .serviceWithZIO[Broker] { broker =>
      broker.put(RequestRunObject(owner, name)).as(Response.text("Run requested"))
    }
    .catchAll(th => ZIO.logError(th.getMessage).as(Response.text(th.getMessage).status(Status.BadRequest)))
