package qmf.poc.service.jsonrpc

import qmf.poc.service.agent.OutgoingMessage

import scala.collection.mutable
import scala.util.Random

trait JsonRpcOutgoingMessagesStore:
  def push(message: OutgoingMessage): Int
  def pop(id: Int): Option[OutgoingMessage]

object JsonRpcOutgoingMessagesStore:
  val live: JsonRpcOutgoingMessagesStore = new JsonRpcOutgoingMessagesStore {
    private val requestsMap: mutable.Map[Int, OutgoingMessage] = mutable.Map()

    def push(message: OutgoingMessage): Int =
      requestsMap.put(message.id, message)
      message.id

    def pop(id: Int): Option[OutgoingMessage] =
      val m = requestsMap.get(id)
      requestsMap.remove(id)
      m
  }
