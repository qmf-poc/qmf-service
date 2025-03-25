package qmf.poc.service.agent

import qmf.poc.service.repository.{Repository, RepositoryError}
import zio.{Promise, Queue, Ref, UIO, URLayer, ZIO, ZLayer}

class BrokerLive(
    outgoingQueue: Queue[OutgoingMessage],
    repository: Repository,
    pending: Ref[Map[Int, Promise[AgentError, IncomingMessage]]]
) extends Broker:
  def handle(error: AgentError): ZIO[OutgoingMessageIdGenerator, Nothing, Unit] = for {
    _ <- ZIO.logDebug(s"broker handles $error")
    _ <- error.outgoingMessage match
      case Some(message: OutgoingMessage) =>
        pending.modify(map => (map.get(message.id), map - message.id)).flatMap {
          case Some(p) => p.fail(error).unit
          case _       => ZIO.unit
        }
      case _ => ZIO.unit
  } yield ()
  def handle(incoming: IncomingMessage): ZIO[OutgoingMessageIdGenerator, RepositoryError, Unit] =
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
          _ <- ZIO.logDebug(s"broker handles pong(payload=$payload, pong=$pong)")
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

  def take: UIO[OutgoingMessage] =
    outgoingQueue.take.tap(m => ZIO.logDebug(s"outgoing message took $m"))

  private def putM(message: OutgoingMessage): UIO[Promise[AgentError, IncomingMessage]] = for {
    promise <- Promise.make[AgentError, IncomingMessage]
    _ <- pending.update(_ + (message.id -> promise))
    _ <- outgoingQueue.offer(message)
    p <- pending.get
    _ <- ZIO.logDebug(s"outgoing message offered $message, pending.length=${p.size}")
  } yield promise

  def put[Req <: OutgoingMessage](message: Req)(using rt: ResponseType[Req]): UIO[Promise[AgentError, rt.Res]] =
    putM(message).map(_.asInstanceOf[Promise[AgentError, rt.Res]])

object BrokerLive:
  val layer: URLayer[Queue[OutgoingMessage] & Repository & Ref[Map[Int, Promise[AgentError, IncomingMessage]]], Broker] =
    ZLayer.fromFunction(BrokerLive(_, _, _))
