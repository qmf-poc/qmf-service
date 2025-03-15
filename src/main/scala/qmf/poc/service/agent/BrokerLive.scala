package qmf.poc.service.agent

import qmf.poc.service.agent.{Alive, Broker, IncomingMessage, OutgoingMessage, Ping, Pong, RequestSnapshot, Snapshot}
import qmf.poc.service.repository.{Repository, RepositoryError}
import zio.{IO, Layer, Queue, Task, UIO, ULayer, URLayer, ZIO, ZLayer}

class BrokerLive(outgoingQueue: Queue[OutgoingMessage], repository: Repository) extends Broker:
  override def handle(incoming: IncomingMessage): IO[RepositoryError, Unit] =
    incoming match
      case Alive(agent) =>
        ZIO.logDebug(s"broker handles alive") *>
          put(Ping("ping on alive"))
        // put(RequestSnapshot("db2inst1", "password"))
        /*
      case ping @ Ping(payload) =>
        ZIO.logDebug(s"broker handles ping") *>
          put(Pong(s"$payload received", ping))
         */
      case pong @ Pong(payload, ping) =>
        ZIO.logDebug(s"broker handles pong(payload=$payload, ping=$ping)")
      case Snapshot(catalog) =>
        for {
          _ <- ZIO.logDebug(s"broker handles catalog $catalog")
          _ <- repository.load(catalog)
        } yield ()
      case RunObjectResult(format, body) =>
        for {
          _ <- ZIO.logDebug(s"broker handles run response $format:$body")
        } yield ()

  override def take: UIO[OutgoingMessage] =
    outgoingQueue.take.tap(m => ZIO.logDebug(s"outgoing message took $m"))

  override def put(message: OutgoingMessage): UIO[Unit] =
    outgoingQueue.offer(message).flatMap(result => ZIO.logDebug(s"outgoing message $message offered $message"))

object BrokerLive:
  val layer: URLayer[Queue[OutgoingMessage] & Repository, Broker] = ZLayer.fromFunction(BrokerLive(_, _))
