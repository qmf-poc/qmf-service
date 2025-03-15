package qmf.poc.service.http.handlers.rest

import qmf.poc.service.agent.{Broker, OutgoingMessageIdGenerator, Ping}
import zio.ZIO
import zio.http.{Request, Response, Status}

def pingAgent: Request => ZIO[Broker & OutgoingMessageIdGenerator, Nothing, Response] = (request: Request) =>
  val payload = s"ping-agent ${System.currentTimeMillis()}"
  for {
    broker <- ZIO.service[Broker]
    id <- ZIO.serviceWithZIO[OutgoingMessageIdGenerator](_.nextId)
    promise <- broker.put(Ping(id, payload))
    pong <- promise.await
    response <- ZIO.succeed(Response.text(s"pong: $pong"))
  } yield response
/*).catchAll(error =>
    ZIO
      .logError(error.getMessage)
      .as(
        Response
          .text(error.getMessage)
          .status(Status.BadRequest)
      )
  )
 */
