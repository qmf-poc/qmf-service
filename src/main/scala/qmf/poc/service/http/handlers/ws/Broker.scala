package qmf.poc.service.http.handlers.ws

import zio.Console.printLine
import zio.{Layer, Queue, Task, UIO, ULayer, ZIO, ZLayer}

trait Broker:
  def handle(incoming: IncomingMessage): Task[Unit]

  def take: UIO[OutgoingMessage]

class BrokerLive(private val outgoingQueue: Queue[OutgoingMessage]) extends Broker:
  override def handle(incoming: IncomingMessage): Task[Unit] =
    (incoming match
      case Alive(agent) => Some(RequestSnapshot(agent, "db2inst1", "password"))
      case Ping(payload) => Some(ReplyPong(s"$payload received"))
      ).fold(ZIO.unit) { value =>
      outgoingQueue.offer(value) *> printLine(s":  ==> $value")
      //ZIO.succeed(outgoingQueue.offer(value))
    }

  override def take: UIO[OutgoingMessage] =
    outgoingQueue.take

object Broker:
  private val outgoingQueue = Queue.sliding[OutgoingMessage](100)
  val layer: Layer[Queue[OutgoingMessage], Broker] =
    ZLayer(outgoingQueue.map(q => new BrokerLive(q)))
