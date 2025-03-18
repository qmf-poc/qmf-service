package qmf.poc.service.agent

import qmf.poc.service.agent.{IncomingMessage, OutgoingMessage}
import qmf.poc.service.jsonrpc.JsonRPCError
import qmf.poc.service.repository.{Repository, RepositoryError}
import zio.{IO, Layer, Promise, Queue, Task, UIO, URLayer, ZIO, ZLayer}

trait ResponseType[Req <: OutgoingMessage] {
  type Res <: IncomingMessage
}

object ResponseType {
  given ResponseType[Ping] with {
    type Res = Pong
  }

  given ResponseType[RequestSnapshot] with {
    type Res = Snapshot
  }

  given ResponseType[RequestRunObject] with {
    type Res = RunObjectResult
  }
}

trait Broker:
  def handle(incoming: IncomingMessage): ZIO[OutgoingMessageIdGenerator, RepositoryError, Unit]

  def handle(error: AgentError): ZIO[OutgoingMessageIdGenerator, Nothing, Unit]

  def take: UIO[OutgoingMessage]

  def put[Req <: OutgoingMessage](message: Req)(using rt: ResponseType[Req]): UIO[Promise[AgentError, rt.Res]]
