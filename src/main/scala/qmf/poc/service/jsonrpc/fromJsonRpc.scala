package qmf.poc.service.jsonrpc

import qmf.poc.service.agent.*
import qmf.poc.service.catalog.CatalogSnapshot
import zio.json.ast.Json
import zio.json.given
import zio.{IO, ZIO}

/** Finds a corresponding request by its id and check if it is of expected type T
  */
private def fromStore(obj: Json.Obj): ZIO[JsonRpcOutgoingMessagesStore, JsonRPCError, OutgoingMessage] =
  ZIO.serviceWithZIO[JsonRpcOutgoingMessagesStore] { store =>
    obj.get("id") match
      case Some(jsonId) =>
        jsonId match
          case Json.Num(idNum) =>
            store.pop(idNum.intValue()) match
              case Some(m) => ZIO.succeed(m) // <---- The only success branch
              case None    => ZIO.fail(JsonRPCError(s"There's no message corresponding to id=$idNum"))
          case n => ZIO.fail(JsonRPCError(s"`id` expected to be int, but $n found"))
      case None => ZIO.fail(JsonRPCError("Expected `id` not found"))
  }

private def paramsString(obj: Json.Obj): IO[JsonRPCError, String] =
  ZIO.fromOption(obj.get("params").flatMap(_.asString)).orElseFail(JsonRPCError("Params not found"))

private def handleMethod(json: Json, obj: Json.Obj) = ???

private def handleResult(resultObj: Json, obj: Json.Obj): ZIO[JsonRpcOutgoingMessagesStore, JsonRPCError, IncomingMessage] =
  for {
    request <- fromStore(obj)
    message <- request match
      case ping: Ping =>
        for {
          payload <- ZIO.fromOption(resultObj.asString).orElseFail(JsonRPCError("result is not string"))
          pong <- ZIO.succeed(Pong(payload, ping))
        } yield pong
      case requestSnapshot: RequestSnapshot =>
        for {
          catalog <- ZIO.fromEither(resultObj.toJson.fromJson[CatalogSnapshot]).mapError(error => JsonRPCError(error))
          snapshot <- ZIO.succeed(Snapshot(catalog, requestSnapshot))
        } yield snapshot
      case requestRunObject: RequestRunObject =>
        for {
          ro <- ZIO.fromOption(resultObj.asObject).orElseFail(JsonRPCError("result is not object"))
          body <- ZIO.fromOption(ro.get("body").flatMap(b => b.asString)).orElseFail(JsonRPCError("result has no body"))
          result <- ZIO.succeed(RunObjectResult("html", body, requestRunObject))
        } yield result
  } yield message

private def extractError(om: OutgoingMessage, resultObj: Json): IO[AgentError | JsonRPCError, Nothing] =
  resultObj.asObject match {
    case Some(obj) => {
      val code = obj.get("code") match
        case Some(Json.Num(code)) => code
        case _                    => -1
      val message = obj.get("message") match
        case Some(Json.Str(message)) => s""""$message""""
        case _                       => "(no message)"
      val data = obj.get("data") match
        case Some(Json.Obj(data)) => data
        case _                    => Json.Obj()
      ZIO.fail(AgentError(s"Error code=$code, message=$message, data=$data. ($om)", Some(om)))
    }
    case None => ZIO.fail(JsonRPCError("Parameter error is not object"))
  }

  // val code = resultObj.asObject
  // ZIO.fail(JsonRPCDecodeError(""))

private def handleError(
    resultObj: Json,
    obj: Json.Obj
): ZIO[JsonRpcOutgoingMessagesStore, AgentError | JsonRPCError, IncomingMessage] =
  fromStore(obj).flatMap(request => extractError(request, resultObj))

def fromJsonRpc(message: String): ZIO[JsonRpcOutgoingMessagesStore, AgentError | JsonRPCError, IncomingMessage] =
  for {
    obj <- ZIO.fromEither(message.fromJson[Json.Obj]).mapError(error => JsonRPCError(error))
    incoming <- (obj.get("method"), obj.get("result"), obj.get("error")) match {
      case (Some(methodObj), _, _) => handleMethod(methodObj, obj)
      case (_, Some(resultObj), _) => handleResult(resultObj, obj)
      case (_, _, Some(resultObj)) => handleError(resultObj, obj)
      case _                       => ZIO.fail(JsonRPCError("Neither method nor result found"))
    }
  } yield incoming
