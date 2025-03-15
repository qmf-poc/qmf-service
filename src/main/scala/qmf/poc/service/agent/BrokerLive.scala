package qmf.poc.service.agent

import qmf.poc.service.repository.{Repository, RepositoryError}
import zio.{Promise, Queue, Ref, UIO, URLayer, ZIO, ZLayer}

class BrokerLive(
    outgoingQueue: Queue[OutgoingMessage],
    repository: Repository,
    pending: Ref[Map[Int, Promise[Nothing, IncomingMessage]]]
) extends Broker:
  override def handle(incoming: IncomingMessage): ZIO[OutgoingMessageIdGenerator, RepositoryError, Unit] =
    incoming match
      case Alive(agent) =>
        for {
          _ <- ZIO.logDebug(s"broker handles alive")
          id <- ZIO.serviceWithZIO[OutgoingMessageIdGenerator] {
            _.nextId
          }
          _ <- put(Ping(id, "ping on alive"))
        } yield ()
      case pong @ Pong(payload, ping) =>
        for {
          _ <- ZIO.logDebug(s"broker handles pong(payload=$payload, ping=$ping)")
          maybePromise <- pending.modify(map => (map.get(ping.id), map - ping.id))
          _ <- maybePromise.fold(ZIO.unit)(_.succeed(pong))
        } yield ()
      case snapshot @ Snapshot(catalog, requestSnapshot) =>
        for {
          _ <- ZIO.logDebug(s"broker handles catalog $catalog")
          _ <- repository.load(catalog)
          maybePromise <- pending.modify(map => (map.get(requestSnapshot.id), map - requestSnapshot.id))
          _ <- maybePromise.fold(ZIO.unit)(_.succeed(snapshot))
        } yield ()
      case result @ RunObjectResult(format, body, requestRunObject) =>
        for {
          _ <- ZIO.logDebug(s"broker handles run response $format:$body")
          maybePromise <- pending.modify(map => (map.get(requestRunObject.id), map - requestRunObject.id))
          _ <- maybePromise.fold(ZIO.unit)(_.succeed(result))
        } yield ()

  override def take: UIO[OutgoingMessage] =
    outgoingQueue.take.tap(m => ZIO.logDebug(s"outgoing message took $m"))

  private def putM(message: OutgoingMessage): UIO[Promise[Nothing, IncomingMessage]] = for {
    promise <- Promise.make[Nothing, IncomingMessage]
    _ <- pending.update(_ + (message.id -> promise))
    _ <- outgoingQueue.offer(message)
    _ <- ZIO.logDebug(s"outgoing message $message offered $message")
  } yield promise

  def put[Req <: OutgoingMessage](message: Req)(using rt: ResponseType[Req]): UIO[Promise[Nothing, rt.Res]] =
    putM(message).map(_.asInstanceOf[Promise[Nothing, rt.Res]])

object BrokerLive:
  val layer: URLayer[Queue[OutgoingMessage] & Repository & Ref[Map[Int, Promise[Nothing, IncomingMessage]]], Broker] =
    ZLayer.fromFunction(BrokerLive(_, _, _))
