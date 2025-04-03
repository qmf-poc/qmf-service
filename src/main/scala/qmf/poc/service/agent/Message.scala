package qmf.poc.service.agent

import qmf.poc.service.catalog.CatalogSnapshot
import zio.{Random, UIO, ULayer, ZLayer}
import zio.json.ast.Json
import zio.json.{DeriveJsonEncoder, JsonCodecConfiguration, JsonEncoder}

sealed trait OutgoingMessage:
  val id: Int
case class Ping(id: Int, payload: String) extends OutgoingMessage
case class RequestSnapshot(id: Int, user: String, password: String) extends OutgoingMessage
case class RequestRunObject(id: Int, user: String, password: String, owner: String, name: String, format: String, limit: Int)
    extends OutgoingMessage

trait OutgoingMessageIdGenerator:
  def nextId: UIO[Int]

object OutgoingMessageIdGenerator:
  val live: OutgoingMessageIdGenerator = new OutgoingMessageIdGenerator:
    private val random = Random.RandomLive
    def nextId: UIO[Int] = random.nextInt

object RequestSnapshot:
  def default(id: Int): RequestSnapshot = RequestSnapshot(id, "db2inst1", "password")

object RequestRunObject:
  def apply(id: Int, owner: String, name: String, limit: Int): RequestRunObject =
    new RequestRunObject(id, "db2inst1", "password", owner, name, "html", limit)

sealed trait IncomingMessage
case class Pong(payload: String, ping: Ping) extends IncomingMessage
case class Alive(agent: String) extends IncomingMessage
case class Snapshot(catalog: CatalogSnapshot, requestSnapshot: RequestSnapshot) extends IncomingMessage
case class RunObjectResult(format: String, body: String, requestRunObject: RequestRunObject) extends IncomingMessage

object MessageJson:
  JsonCodecConfiguration()

  given JsonEncoder[Ping] = DeriveJsonEncoder.gen[Ping]
  given JsonEncoder[OutgoingMessage] = JsonEncoder[Json.Obj].contramap {
    case Ping(id, payload) =>
      Json.Obj(
        "payload" -> Json.Str(payload)
      )
    case RequestSnapshot(id, user, password) =>
      Json.Obj(
        "user" -> Json.Str(user),
        "password" -> Json.Str(password)
      )
    case RequestRunObject(id, user, password, owner, name, format, limit) =>
      Json.Obj(
        "user" -> Json.Str(user),
        "password" -> Json.Str(password),
        "owner" -> Json.Str(owner),
        "name" -> Json.Str(name),
        "format" -> Json.Str(format),
        "limit" -> Json.Num(limit)
      )
  }
