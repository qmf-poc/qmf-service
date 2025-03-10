package qmf.poc.service.http.handlers.ws

import qmf.poc.service.repository.{Repository, RepositoryError}
import zio.{IO, Layer, Queue, Task, UIO, URLayer, ZIO, ZLayer}

trait Broker:
  def handle(incoming: IncomingMessage): IO[RepositoryError, Unit]

  def take: UIO[OutgoingMessage]

  def put(message: OutgoingMessage): Task[Unit]
