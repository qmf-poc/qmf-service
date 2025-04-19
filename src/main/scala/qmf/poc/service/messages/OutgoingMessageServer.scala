package qmf.poc.service.messages

import qmf.poc.service.agent.{AgentId, AgentsRegistry}
import zio.{Queue, UIO, ZIO}

trait OutgoingMessageServer:
  def serve: UIO[Nothing]
