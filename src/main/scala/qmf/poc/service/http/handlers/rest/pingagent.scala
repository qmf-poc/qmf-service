package qmf.poc.service.http.handlers.rest

import qmf.poc.service.agent.{Broker, Ping}
import zio.ZIO
import zio.http.{Request, Response, Status}

def pingAgent: Request => ZIO[Broker, Nothing, Response] = (request: Request) =>
  val payload = s"ping-agent ${System.currentTimeMillis()}"
  ZIO.serviceWithZIO[Broker](broker => broker.put(Ping(payload))).as(Response.text(s"ping: $payload")).catchAll { error =>
    ZIO.logError(error.getMessage).as {
      Response
        .text(error.getMessage)
        .status(Status.BadRequest)
    }
  }
