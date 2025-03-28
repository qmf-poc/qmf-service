package qmf.poc.service.agent

// TODO: misleading naming, should be HandleError or somthing
case class AgentError(message: String, outgoingMessage: Option[OutgoingMessage])
