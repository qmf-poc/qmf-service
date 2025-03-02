package qmf.poc.service.http.handlers.ws

import qmf.poc.service.catalog.{CatalogSnapshot, ObjectData, ObjectDirectory, ObjectRemarks}
import zio.json.ast.Json
import zio.json.{DeriveJsonDecoder, JsonDecoder, given}
import scala.math.Numeric.Implicits.infixNumericOps

import java.util.Base64

/** --> Ping -> Pong --> Alive -> RequestSnapshot --> Snapshot -> (*)
  */
trait OutgoingMessage:
  val jsonrpc: String

case class ReplyPong(payload: String) extends OutgoingMessage:
  override val jsonrpc: String =
    s"""{"jsonrpc": "2.0", "method": "pong", "params": "$payload"}"""

case class RequestSnapshot(user: String, password: String) extends OutgoingMessage:
  override val jsonrpc: String =
    s"""{"jsonrpc": "2.0", "method": "requestSnapshot", "params": {"user": "$user", "password": "$password"}}"""
    
object RequestSnapshot:
  val default: RequestSnapshot = RequestSnapshot("db2inst1", "password")

trait IncomingMessage

case class Ping(payload: String) extends IncomingMessage

case class Alive(agent: String) extends IncomingMessage

// case class Snapshot(agent: String, catalog: CatalogSnapshot) extends IncomingMessage
case class Snapshot(catalog: CatalogSnapshot) extends IncomingMessage

object IncomingMessage:
  given JsonDecoder[ObjectData] = JsonDecoder[Json.Obj].mapOrFail(jsonObj =>
    for {
      owner <- jsonObj.get("owner").toRight("Missing owner").flatMap {
        case Json.Str(value) => Right(value)
        case _               => Left("Invalid owner")
      }
      name <- jsonObj.get("name").toRight("Missing name").flatMap {
        case Json.Str(value) => Right(value)
        case _               => Left("Invalid name")
      }
      objType <- jsonObj.get("type").toRight("Missing type").flatMap {
        case Json.Str(value) => Right(value)
        case _               => Left("Invalid type")
      }
      seq <- jsonObj.get("seq").toRight("Missing seq").flatMap {
        case Json.Num(value) => Right(value.shortValue())
        case _               => Left("Invalid seq: must be a number")
      }
      appldata <- jsonObj.get("appldata").toRight("Missing appldata").flatMap {
        case Json.Str(base64Str) =>
          try {
            Right(Base64.getDecoder.decode(base64Str)) // Successfully decode the base64 string
          } catch {
            case e: IllegalArgumentException =>
              Left(s"Failed to decode appldata: ${e.getMessage}") // Handle decoding failures
          }
        case _ => Left("invalid appldata")
      }
    } yield ObjectData(owner, name, objType, seq, appldata)

    /*
        try {
          val decodedData = Base64.getDecoder.decode(objectData.appldata)
          Right(objectData.copy(appldata = decodedData))
        } catch {
          case e: Exception => Left(s"Failed to decode base64: ${e.getMessage}")
        }
     */
  )
  given JsonDecoder[ObjectRemarks] = DeriveJsonDecoder.gen[ObjectRemarks]
  given JsonDecoder[ObjectDirectory] = DeriveJsonDecoder.gen[ObjectDirectory]
  given JsonDecoder[CatalogSnapshot] = DeriveJsonDecoder.gen[CatalogSnapshot].mapOrFail { obj =>
    Right(obj.copy())
  }

  given JsonDecoder[IncomingMessage] = JsonDecoder[Json.Obj].mapOrFail { obj =>
    for {
      methodObj <- obj.get("method").toRight("Method not found")

      method <- methodObj match {
        case Json.Str(value) => Right(value)
        case _               => Left("Method must be a string")
      }

      paramsObj <- obj.get("params").toRight("Params not found")

      message <- method match
        case "ping" =>
          paramsObj match
            case Json.Str(payload) => Right(Ping(payload))
            case _                 => Left("params must be string")
        case "alive" =>
          paramsObj match
            case Json.Str(agent) => Right(Alive(agent))
            case _               => Left("params must be string")
        case "snapshot" =>
          paramsObj match
            case Json.Obj(_) =>
              paramsObj.toJson.fromJson[CatalogSnapshot].map(snapshot => Snapshot(snapshot))
            case _ => Left("params must be an object")
    } yield message
  }
