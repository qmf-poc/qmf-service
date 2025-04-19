package qmf.poc.service.messages

import qmf.poc.service.qmfstorage.QmfObjectsStorageError
import zio.{IO, UIO, ZIO}

trait IncomingMessageHandler:
  def handle(incoming: IncomingMessage): IO[QmfObjectsStorageError, Unit]

  def handle(error: OutgoingMessageError): UIO[Unit]
