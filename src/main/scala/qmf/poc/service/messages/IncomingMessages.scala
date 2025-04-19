package qmf.poc.service.messages

import qmf.poc.service.agent.*
import qmf.poc.service.catalog.CatalogSnapshot
import zio.json.ast.Json
import zio.json.{DeriveJsonEncoder, JsonCodecConfiguration, JsonDecoder, JsonEncoder}
import zio.{Random, UIO, ULayer, ZLayer}

sealed trait IncomingMessage
case class ImPong(payload: String, ping: OmPing) extends IncomingMessage
case class ImAlive(agent: AgentId) extends IncomingMessage
case class ImSnapshot(catalog: CatalogSnapshot, requestSnapshot: OmRequestSnapshot) extends IncomingMessage
case class ImRunObjectResult(format: String, body: String, requestRunObject: OmRequestRunObject) extends IncomingMessage

object MessageJson:
  // JsonCodecConfiguration()

  given JsonEncoder[OmPing] = DeriveJsonEncoder.gen[OmPing]
  given JsonEncoder[OutgoingMessage] = JsonEncoder[Json.Obj].contramap {
    case OmPing(id, payload) =>
      Json.Obj(
        "payload" -> Json.Str(payload)
      )
    case OmRequestSnapshot(id, user, password) =>
      Json.Obj(
        "user" -> Json.Str(user),
        "password" -> Json.Str(password)
      )
    case OmRequestRunObject(id, user, password, owner, name, format, limit) =>
      Json.Obj(
        "user" -> Json.Str(user),
        "password" -> Json.Str(password),
        "owner" -> Json.Str(owner),
        "name" -> Json.Str(name),
        "format" -> Json.Str(format),
        "limit" -> Json.Num(limit)
      )
  }
