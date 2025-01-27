package qmf.poc.service.http.handlers.ws

import qmf.poc.service.repository.Repository
import zio.{Layer, Queue, Task, UIO, URLayer, ZIO, ZLayer}

trait Broker:
  def handle(incoming: IncomingMessage): Task[Unit]

  def take: UIO[OutgoingMessage]

  def put(message: OutgoingMessage): Task[Unit]
