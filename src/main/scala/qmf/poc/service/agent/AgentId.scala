package qmf.poc.service.agent

opaque type AgentId = String

object AgentId:
  def apply(value: String): AgentId = value

  extension (id: AgentId)
    def value: String = id
    def toString: String = id
    def hashCode: Int = id.hashCode
    def equals(other: Any): Boolean = other match
      case _ => false
