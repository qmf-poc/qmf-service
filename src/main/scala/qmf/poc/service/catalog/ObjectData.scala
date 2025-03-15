package qmf.poc.service.catalog

import zio.json.JsonDecoder
import zio.json.ast.Json

case class ObjectData(owner: String, name: String, `type`: String, seq: Short, appldata: Array[Byte])

object ObjectData:
  def ObjectData(jsonObj: Json.Obj): Either[String, ObjectData] =
    import java.util.Base64
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
    } yield new ObjectData(owner, name, objType, seq, appldata)

  given JsonDecoder[ObjectData] = JsonDecoder[Json.Obj].mapOrFail(jsonObj => ObjectData(jsonObj))
