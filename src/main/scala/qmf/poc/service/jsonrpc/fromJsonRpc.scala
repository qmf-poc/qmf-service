package qmf.poc.service.jsonrpc

import qmf.poc.service.agent.*
import qmf.poc.service.catalog.CatalogSnapshot
import zio.json.ast.Json
import zio.json.given
import zio.{IO, ZIO}

/** Finds a corresponding request by its id and check if it is of expected type T
  */
private def fromStore(obj: Json.Obj): ZIO[JsonRpcOutgoingMessagesStore, JsonRPCDecodeError, OutgoingMessage] =
  ZIO.serviceWithZIO[JsonRpcOutgoingMessagesStore] { store =>
    obj.get("id") match
      case Some(jsonId) =>
        jsonId match
          case Json.Num(idNum) =>
            store.pop(idNum.intValue()) match
              case Some(m) => ZIO.succeed(m) // <---- The only success branch
              case None    => ZIO.fail(JsonRPCDecodeError(s"There's no message corresponding to id=$idNum"))
          case n => ZIO.fail(JsonRPCDecodeError(s"`id` expected to be int, but $n found"))
      case None => ZIO.fail(JsonRPCDecodeError("Expected `id` not found"))
  }

private def paramsString(obj: Json.Obj): IO[JsonRPCDecodeError, String] =
  ZIO.fromOption(obj.get("params").flatMap(_.asString)).orElseFail(JsonRPCDecodeError("Params not found"))

private def handleMethod(json: Json, obj: Json.Obj) = ???

private def handleResult(resultObj: Json, obj: Json.Obj): ZIO[JsonRpcOutgoingMessagesStore, JsonRPCDecodeError, IncomingMessage] =
  for {
    request <- fromStore(obj)
    message <- request match
      case ping: Ping =>
        for {
          payload <- ZIO.fromOption(resultObj.asString).orElseFail(JsonRPCDecodeError("result is not string"))
          pong <- ZIO.succeed(Pong(payload, ping))
        } yield pong
      case snapshot: RequestSnapshot =>
        for {
          catalog <- ZIO.fromEither(resultObj.toJson.fromJson[CatalogSnapshot]).mapError(error => JsonRPCDecodeError(error))
          snapshot <- ZIO.succeed(Snapshot(catalog))
        } yield snapshot
      case runResult: RequestRunObject =>
        for {
          ro <- ZIO.fromOption(resultObj.asObject).orElseFail(JsonRPCDecodeError("result is not object"))
          body <- ZIO.fromOption(ro.get("body").flatMap(b => b.asString)).orElseFail(JsonRPCDecodeError("result has no body"))
          result <- ZIO.succeed(RunObjectResult("html", body))
        } yield result
  } yield message

def fromJsonRpc(message: String): ZIO[JsonRpcOutgoingMessagesStore, JsonRPCDecodeError, IncomingMessage] =
  for {
    obj <- ZIO.fromEither(message.fromJson[Json.Obj]).mapError(error => JsonRPCDecodeError(error))
    incoming <- (obj.get("method"), obj.get("result")) match {
      case (Some(methodObj), _) => handleMethod(methodObj, obj)
      case (_, Some(resultObj)) => handleResult(resultObj, obj)
      case _                    => ZIO.fail(JsonRPCDecodeError("Neither method nor result found"))
    }
  } yield incoming
