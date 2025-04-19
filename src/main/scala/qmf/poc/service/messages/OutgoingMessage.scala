package qmf.poc.service.messages

import qmf.poc.service.agent.*
import qmf.poc.service.catalog.CatalogSnapshot
import zio.json.ast.Json
import zio.json.{DeriveJsonEncoder, JsonCodecConfiguration, JsonDecoder, JsonEncoder}
import zio.{Random, UIO, ULayer, ZLayer}

sealed trait OutgoingMessage:
  val id: Int
case class OmPing(id: Int, payload: String) extends OutgoingMessage
case class OmRequestSnapshot(id: Int, user: String, password: String) extends OutgoingMessage
case class OmRequestRunObject(id: Int, user: String, password: String, owner: String, name: String, format: String, limit: Int)
    extends OutgoingMessage

object OmRequestSnapshot:
  def default(id: Int): OmRequestSnapshot = OmRequestSnapshot(id, "db2inst1", "password")

object OmRequestRunObject:
  def apply(id: Int, owner: String, name: String, limit: Int): OmRequestRunObject =
    new OmRequestRunObject(id, "db2inst1", "password", owner, name, "html", limit)
