package qmf.poc.service.agent

import qmf.poc.service.messages.OutgoingMessage

trait Agent:
  def send(message: OutgoingMessage): Unit
