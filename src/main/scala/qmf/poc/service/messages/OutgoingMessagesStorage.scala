package qmf.poc.service.messages

import qmf.poc.service.messages.{IncomingMessage, OutgoingMessage, OutgoingMessageError}
import zio.{IO, Promise, Ref, UIO, ZIO, ZLayer}

import scala.collection.mutable

trait OutgoingMessageNotFoundError
object OutgoingMessageNotFoundError extends OutgoingMessageNotFoundError

trait OutgoingMessagesStorage:
  def push(message: OutgoingMessage): UIO[Int]
  def pop(id: Int): IO[OutgoingMessageNotFoundError, OutgoingMessage]

object OutgoingMessagesStorage:
  val live: UIO[OutgoingMessagesStorage] = Ref.make(Map.empty[Int, OutgoingMessage]).map { ref =>
    new OutgoingMessagesStorage:
      def push(message: OutgoingMessage): UIO[Int] =
        ref.updateAndGet(_ + (message.id -> message)).as(message.id)

      def pop(id: Int): IO[OutgoingMessageNotFoundError, OutgoingMessage] =
        ref
          .modify(map => (map.get(id), map - id))
          .flatMap {
            case Some(m) => ZIO.succeed(m)
            case None    => ZIO.fail(OutgoingMessageNotFoundError)
          }
  }
