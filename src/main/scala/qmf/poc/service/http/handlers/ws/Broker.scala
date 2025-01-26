package qmf.poc.service.http.handlers.ws

import zio.{Layer, Queue, Task, UIO, ZIO, ZLayer}

trait Broker:
  def handle(incoming: IncomingMessage): Task[Unit]

  def take: UIO[OutgoingMessage]
  
  def put(message: OutgoingMessage):Task[Unit]

class BrokerLive(private val outgoingQueue: Queue[OutgoingMessage]) extends Broker:
  override def handle(incoming: IncomingMessage): Task[Unit] =
    incoming match
      case Alive(agent) => put(RequestSnapshot("db2inst1", "password")).unit
      case Ping(payload) => put(ReplyPong(s"$payload received")).unit

  override def take: UIO[OutgoingMessage] =
    outgoingQueue.take

  def put(message: OutgoingMessage):Task[Unit] =
    outgoingQueue.offer(message).unit

object Broker:
  private val outgoingQueue = Queue.sliding[OutgoingMessage](100)
  val layer: Layer[Queue[OutgoingMessage], Broker] =
    ZLayer(outgoingQueue.map(q => new BrokerLive(q)))
