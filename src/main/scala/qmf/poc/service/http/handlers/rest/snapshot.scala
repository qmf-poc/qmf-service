package qmf.poc.service.http.handlers.rest

import qmf.poc.service.agent.{Broker, RequestSnapshot}
import zio.ZIO
import zio.http.{Request, Response, Status}

def snapshot: Request => ZIO[Broker, Nothing, Response] = (request: Request) =>
  ZIO
    .serviceWithZIO[Broker] { broker =>
      broker.put(RequestSnapshot.default).as(Response.text("Refresh requested"))
    }
    .catchAll(th => ZIO.logError(th.getMessage).as(Response.text(th.getMessage).status(Status.BadRequest)))
