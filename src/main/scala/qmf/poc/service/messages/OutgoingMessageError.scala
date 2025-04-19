package qmf.poc.service.messages

import qmf.poc.service.messages.OutgoingMessage

// TODO: probably misleading naming
case class OutgoingMessageError(message: String, outgoingMessage: Option[OutgoingMessage])
