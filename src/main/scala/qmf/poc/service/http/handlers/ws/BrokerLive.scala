package qmf.poc.service.http.handlers.ws

import qmf.poc.service.repository.Repository
import zio.{Layer, Queue, Task, UIO, ULayer, URLayer, ZIO, ZLayer}

class BrokerLive(outgoingQueue: Queue[OutgoingMessage], repository: Repository) extends Broker:
  override def handle(incoming: IncomingMessage): Task[Unit] =
    incoming match
      case Alive(agent) =>
        ZIO.logDebug(s"broker handles alive") *>
          put(RequestSnapshot("db2inst1", "password"))
      case Ping(payload) =>
        ZIO.logDebug(s"broker handles ping") *>
          put(ReplyPong(s"$payload received"))
      case Snapshot(catalog) =>
        for {
          _ <- ZIO.logDebug(s"broker handles catalog $catalog")
          _ <- repository.load(catalog)
        } yield ()

  override def take: UIO[OutgoingMessage] =
    outgoingQueue.take.tap(m => ZIO.logDebug(s"outgoind message took $m"))

  override def put(message: OutgoingMessage): UIO[Unit] =
    outgoingQueue.offer(message).flatMap(result => ZIO.logDebug(s"outgoing message $message offered $message"))

object BrokerLive:
  val layer: URLayer[Queue[OutgoingMessage] & Repository, Broker] = ZLayer.fromFunction(BrokerLive(_, _))
