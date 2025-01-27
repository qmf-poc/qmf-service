package qmf.poc.service.http.handlers.ws

import qmf.poc.service.repository.Repository
import zio.{Layer, Queue, Task, UIO, ULayer, URLayer, ZIO, ZLayer}

class BrokerLive(outgoingQueue: Queue[OutgoingMessage], repository: Repository) extends Broker:
  override def handle(incoming: IncomingMessage): Task[Unit] =
    incoming match
      case Alive(agent) => put(RequestSnapshot("db2inst1", "password")).unit
      case Ping(payload) => put(ReplyPong(s"$payload received")).unit
      case Snapshot(catalog) =>
        ZIO.logDebug(s"catalog received $catalog") *> repository.load(catalog) *> ZIO.logDebug(s"catalog loaded")

  override def take: UIO[OutgoingMessage] =
    outgoingQueue.take

  def put(message: OutgoingMessage): Task[Unit] =
    outgoingQueue.offer(message).unit

object BrokerLive:
  val layer: URLayer[Queue[OutgoingMessage] & Repository, Broker] = ZLayer.fromFunction(BrokerLive(_, _))
