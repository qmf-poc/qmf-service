package qmf.poc.service.http.handlers.ws

import qmf.poc.service.repository.Repository
import zio.{Layer, Queue, Task, UIO, ULayer, URLayer, ZIO, ZLayer}

class BrokerLive(outgoingQueue: Queue[OutgoingMessage], repository: Repository) extends Broker:
  override def handle(incoming: IncomingMessage): Task[Unit] =
    incoming match
      case Alive(agent) =>
        ZIO.logDebug(s"broker handles alive") *>
          put(RequestSnapshot("db2inst1", "password")).unit
      case Ping(payload) =>
        put(ReplyPong(s"$payload received")).unit
      case Snapshot(catalog) =>
        for {
          _ <- ZIO.logDebug("catalog received catalog").flatMap(r => ZIO.logWarning("ss"))
          _ <- repository.load(catalog)
          l <- ZIO.logDebug("catalog loaded")
        } yield l

  override def take: UIO[OutgoingMessage] =
    outgoingQueue.take.tap(m => ZIO.logDebug(s"message took $m"))

  def put(message: OutgoingMessage): Task[Unit] =
    ZIO.logDebug(s"put outgoing message $message") *>
      outgoingQueue.offer(message).unit

object BrokerLive:
  val layer: URLayer[Queue[OutgoingMessage] & Repository, Broker] = ZLayer.fromFunction(BrokerLive(_, _))
