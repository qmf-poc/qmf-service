package qmf.poc.service.http.handlers.ws

import qmf.poc.service.catalog.Catalog

/**
 * --> Ping -> Pong
 * --> Alive -> RequestSnapshot
 * --> Snapshot -> (*)
 */
trait OutgoingMessage:
  val jsonrpc: String

case class ReplyPong(payload: String) extends OutgoingMessage:
  override val jsonrpc: String =
    s"""{"jsonrpc": "2.0", "method": "pong", "params": "$payload"}"""

case class RequestSnapshot(user: String, password: String) extends OutgoingMessage:
  override val jsonrpc: String =
    s"""{"jsonrpc": "2.0", "method": "requestSnapshot", "params": {"user": "$user", "password": "$password"}}"""

trait IncomingMessage

case class Ping(payload: String) extends IncomingMessage

case class Alive(agent: String) extends IncomingMessage

case class Snapshot(agent: String, catalog: Catalog) extends IncomingMessage
