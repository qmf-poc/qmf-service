package qmf.poc.service.agent

case class AgentError(message: String, outgoingMessage: Option[OutgoingMessage])
