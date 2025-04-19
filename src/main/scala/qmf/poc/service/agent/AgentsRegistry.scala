package qmf.poc.service.agent

import zio.UIO

trait AgentsRegistry:
  def configuredAgents: UIO[List[AgentId]]
  def activeAgents: UIO[List[AgentId]]
  def registerAgent(agentId: AgentId, agent: Agent): UIO[Unit]
  def unregisterAgent(agentId: AgentId): UIO[Unit]
  def getAgent(agentId: AgentId): UIO[Option[Agent]]
