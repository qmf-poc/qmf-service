package qmf.poc.service.agent

import qmf.poc.service.catalog.{CatalogSnapshot, ObjectData, ObjectDirectory, ObjectRemarks}
import zio.json.ast.Json
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonCodecConfiguration, JsonDecoder, JsonEncoder}

sealed trait OutgoingMessage
case class Ping(payload: String) extends OutgoingMessage
case class RequestSnapshot(user: String, password: String) extends OutgoingMessage
case class RequestRunObject(user: String, password: String, owner: String, name: String, format: String) extends OutgoingMessage

object RequestSnapshot:
  val default: RequestSnapshot = RequestSnapshot("db2inst1", "password")

object RequestRunObject:
  def apply(owner: String, name: String): RequestRunObject =
    new RequestRunObject("db2inst1", "password", owner, name, "html")

sealed trait IncomingMessage
case class Pong(payload: String, ping: Ping) extends IncomingMessage
case class Alive(agent: String) extends IncomingMessage
case class Snapshot(catalog: CatalogSnapshot) extends IncomingMessage
case class RunObjectResult(format: String, body: String) extends IncomingMessage

object MessageJson:
  JsonCodecConfiguration()

  given JsonEncoder[Ping] = DeriveJsonEncoder.gen[Ping]
  given JsonEncoder[OutgoingMessage] = JsonEncoder[Json.Obj].contramap {
    case Ping(payload) =>
      Json.Obj(
        "payload" -> Json.Str(payload)
      )
    case RequestSnapshot(user, password) =>
      Json.Obj(
        "user" -> Json.Str(user),
        "password" -> Json.Str(password)
      )
    case RequestRunObject(user, password, owner, name, format) =>
      Json.Obj(
        "user" -> Json.Str(user),
        "password" -> Json.Str(password),
        "owner" -> Json.Str(owner),
        "name" -> Json.Str(name),
        "format" -> Json.Str(format)
      )
  }
