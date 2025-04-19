package qmf.poc.service.messages

import qmf.poc.service.agent.{AgentId, AgentsRegistry}
import zio.{Queue, UIO, URLayer, ZIO, ZLayer}

class OutgoingMessageServerLive(outgoingQueue: Queue[(AgentId, OutgoingMessage)], agentsRegistry: AgentsRegistry)
    extends OutgoingMessageServer {
  override def serve: UIO[Nothing] =
    outgoingQueue.take.flatMap { (agentId, message) =>
      agentsRegistry.getAgent(agentId).flatMap {
        case Some(agent) =>
          ZIO.succeed(agent.send(message))
        case None =>
          ZIO.logWarning(s"Agent not found: $agentId")
      }
    }.forever
}

object OutgoingMessageServerLive {
  def apply(outgoingQueue: Queue[(AgentId, OutgoingMessage)], agentsRegistry: AgentsRegistry): OutgoingMessageServer =
    new OutgoingMessageServerLive(outgoingQueue, agentsRegistry)

  def layer: URLayer[Queue[(AgentId, OutgoingMessage)] & AgentsRegistry, OutgoingMessageServer] =
    ZLayer.fromFunction(OutgoingMessageServerLive(_, _))
}
