package qmf.poc.service.http.handlers.ws

trait OutgoingMessage:
  val jsonrpc: String

case class RequestSnapshot(connectionString: String, user: String, password: String) extends OutgoingMessage:
  override val jsonrpc: String =
    s"""{"jsonrpc": "2.0", "method": "requestSnapshot", "params": {connectionString: "$connectionString", user: "$user", password: "$password"}}"""

case class ReplyPong(payload: String) extends OutgoingMessage:
  override val jsonrpc: String =
    s"""{"jsonrpc": "2.0", "method": pong", "params": "$payload"}"""

trait IncomingMessage

case class Snapshot(catalog: String) extends IncomingMessage

case class Ping(payload: String) extends IncomingMessage

case class Alive(agent: String) extends IncomingMessage
