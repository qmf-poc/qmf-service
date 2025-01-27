package qmf.poc.service.http.handlers.ws

import qmf.poc.service.catalog.{CatalogSnapshot, ObjectData, ObjectDirectory, ObjectRemarks}
import zio.json.ast.Json
import zio.json.{DeriveJsonDecoder, JsonDecoder, given }

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

// case class Snapshot(agent: String, catalog: CatalogSnapshot) extends IncomingMessage
case class Snapshot(catalog: CatalogSnapshot) extends IncomingMessage

object IncomingMessage:
  given JsonDecoder[ObjectData] = DeriveJsonDecoder.gen[ObjectData]
  given JsonDecoder[ObjectRemarks] = DeriveJsonDecoder.gen[ObjectRemarks]
  given JsonDecoder[ObjectDirectory] = DeriveJsonDecoder.gen[ObjectDirectory]
  given JsonDecoder[CatalogSnapshot] = DeriveJsonDecoder.gen[CatalogSnapshot]

  given JsonDecoder[IncomingMessage] = JsonDecoder[Json.Obj].mapOrFail { obj =>
    for {
      methodObj <- obj.get("method").toRight("Method not found")

      method <- methodObj match {
        case Json.Str(value) => Right(value)
        case _ => Left("Method must be a string")
      }

      paramsObj <- obj.get("params").toRight("Params not found")

      message <- method match
        case "ping" => paramsObj match
          case Json.Str(payload) => Right(Ping(payload))
          case _ => Left("params must be string")
        case "alive" => paramsObj match
          case Json.Str(agent) => Right(Alive(agent))
          case _ => Left("params must be string")
        case "snapshot" => paramsObj match
          case Json.Obj(_) =>
            paramsObj.toJson.fromJson[CatalogSnapshot].map(snapshot => Snapshot(snapshot))
          case _ => Left("params must be an object")
    } yield message
  }
