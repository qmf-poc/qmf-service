package qmf.poc.service.messages

import qmf.poc.service.agent.*
import qmf.poc.service.qmfstorage.{QmfObjectsStorage, QmfObjectsStorageError}
import zio.{IO, Promise, Ref, UIO, ZIO, ZLayer}

class IncomingMessageHandlerLive(
    repository: QmfObjectsStorage,
    pendingRequestsPromises: Ref[Map[Int, Promise[OutgoingMessageError, IncomingMessage]]]
) extends IncomingMessageHandler {
  def handle(error: OutgoingMessageError): UIO[Unit] = for {
    _ <- ZIO.logDebug(s"broker handles ${error.message} for outgoing message: ${error.outgoingMessage}")
    _ <- error.outgoingMessage match
      case Some(message: OutgoingMessage) =>
        pendingRequestsPromises.modify(map => (map.get(message.id), map - message.id)).flatMap {
          case Some(p) => ZIO.logDebug(s"Fail pending id=${message.id}") *> p.fail(error).unit
          case _       => ZIO.unit
        }
      case _ => ZIO.unit
  } yield ()
  def handle(incoming: IncomingMessage): IO[QmfObjectsStorageError, Unit] =
    incoming match
      case ImAlive(agent) => ZIO.logDebug(s"broker skips alive $agent")
      case pong @ ImPong(payload, ping) =>
        for {
          _ <- ZIO.logDebug(s"broker handles pong(payload=$payload, pong=$pong)")
          maybePromise <- pendingRequestsPromises.modify(map => (map.get(ping.id), map - ping.id))
          _ <- maybePromise.fold(ZIO.unit)(_.succeed(pong))
        } yield ()
      case snapshot @ ImSnapshot(catalog, requestSnapshot) =>
        for {
          _ <- ZIO.logDebug(s"broker handles catalog $catalog")
          _ <- repository.load(catalog)
          // TODO: error deos not cause pending to
          maybePromise <- pendingRequestsPromises.modify(map => (map.get(requestSnapshot.id), map - requestSnapshot.id))
          _ <- maybePromise.fold(ZIO.unit)(_.succeed(snapshot))
        } yield ()
      case result @ ImRunObjectResult(format, body, requestRunObject) =>
        for {
          _ <- ZIO.logDebug(s"broker handles run response $format:$body")
          maybePromise <- pendingRequestsPromises.modify(map => (map.get(requestRunObject.id), map - requestRunObject.id))
          _ <- maybePromise.fold(ZIO.unit)(_.succeed(result))
        } yield ()
}

object IncomingMessageHandlerLive {
  def layer
      : ZLayer[QmfObjectsStorage & Ref[Map[Int, Promise[OutgoingMessageError, IncomingMessage]]], Nothing, IncomingMessageHandler] =
    ZLayer.fromFunction(IncomingMessageHandlerLive(_, _))
}

/*
def apply(
           repository: QmfObjectsStorage,
           pendingRequests: Ref[Map[Int, Promise[OutgoingMessageError, IncomingMessage]]]
): IncomingMessageHandler =
  new IncomingMessageHandlerLive(repository, pendingRequests)
 */
