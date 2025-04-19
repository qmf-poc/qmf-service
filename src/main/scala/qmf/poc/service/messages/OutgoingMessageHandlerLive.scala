package qmf.poc.service.messages

import qmf.poc.service.agent.AgentId
import qmf.poc.service.qmfstorage.{QmfObjectsStorage, QmfObjectsStorageError}
import zio.{Promise, Queue, Ref, UIO, URLayer, ZIO, ZLayer}

class OutgoingMessageHandlerLive(
    outgoingQueue: Queue[(AgentId, OutgoingMessage)],
    pendingRequestsPromises: Ref[Map[Int, Promise[OutgoingMessageError, IncomingMessage]]]
) extends OutgoingMessageHandler:
  private def putM(agentId: AgentId, message: OutgoingMessage): UIO[Promise[OutgoingMessageError, IncomingMessage]] = for {
    promise <- Promise.make[OutgoingMessageError, IncomingMessage]
    _ <- pendingRequestsPromises.update(_ + (message.id -> promise))
    _ <- ZIO.logDebug(s"pending promise added: ${message.id}")
    _ <- outgoingQueue.offer((agentId, message))
    p <- pendingRequestsPromises.get
    _ <- ZIO.logDebug(s"outgoing message offered $message, pending.length=${p.size}")
  } yield promise

  def put[Req <: OutgoingMessage](agentId: AgentId, message: Req)(using
      rt: ResponseType[Req]
  ): UIO[Promise[OutgoingMessageError, rt.Res]] =
    putM(agentId, message).map(_.asInstanceOf[Promise[OutgoingMessageError, rt.Res]])

object OutgoingMessageHandlerLive:
  val layer: URLayer[Queue[
    (AgentId, OutgoingMessage)
  ] & Ref[Map[Int, Promise[OutgoingMessageError, IncomingMessage]]], OutgoingMessageHandler] =
    ZLayer.fromFunction(OutgoingMessageHandlerLive(_, _))
  val live: ZLayer[Ref[Map[Int, Promise[OutgoingMessageError, IncomingMessage]]], Nothing, OutgoingMessageHandler] =
    ZLayer(Queue.sliding[(AgentId, OutgoingMessage)](100)) >>> layer
