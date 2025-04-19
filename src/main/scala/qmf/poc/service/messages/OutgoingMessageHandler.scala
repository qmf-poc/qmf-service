package qmf.poc.service.messages

import qmf.poc.service.agent.AgentId
import qmf.poc.service.jsonrpc.JsonRPCError
import qmf.poc.service.qmfstorage.{QmfObjectsStorage, QmfObjectsStorageError}
import zio.{IO, Layer, Promise, Queue, Task, UIO, URLayer, ZIO, ZLayer}

trait ResponseType[Req <: OutgoingMessage] {
  type Res <: IncomingMessage
}

object ResponseType {
  given ResponseType[OmPing] with {
    type Res = ImPong
  }

  given ResponseType[OmRequestSnapshot] with {
    type Res = ImSnapshot
  }

  given ResponseType[OmRequestRunObject] with {
    type Res = ImRunObjectResult
  }
}

trait OutgoingMessageHandler:
  def put[Req <: OutgoingMessage](agentId: AgentId, message: Req)(using
      rt: ResponseType[Req]
  ): UIO[Promise[OutgoingMessageError, rt.Res]]
