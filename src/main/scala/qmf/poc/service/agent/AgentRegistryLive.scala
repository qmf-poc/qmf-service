package qmf.poc.service.agent

import zio.{Ref, UIO, ZIO, ZLayer}

final class AgentRegistryLive private (agents: Ref[Map[AgentId, Agent]]) extends AgentsRegistry:
  override def configuredAgents: UIO[List[AgentId]] =
    ZIO.succeed(List(AgentId("agent1"), AgentId("agent2")))

  override def activeAgents: UIO[List[AgentId]] =
    agents.get.map(_.keys.toList)

  override def registerAgent(agentId: AgentId, agent: Agent): UIO[Unit] =
    agents.update(_ + (agentId -> agent))

  override def unregisterAgent(agentId: AgentId): UIO[Unit] =
    agents.update(_ - agentId)

  override def getAgent(agentId: AgentId): UIO[Option[Agent]] =
    agents.get.map(_.get(agentId))

object AgentRegistryLive:
  private def make: UIO[AgentRegistryLive] =
    Ref.make(Map.empty[AgentId, Agent]).map(new AgentRegistryLive(_))
  def layer: ZLayer[Any, Nothing, AgentsRegistry] =
    ZLayer.fromZIO(make)
