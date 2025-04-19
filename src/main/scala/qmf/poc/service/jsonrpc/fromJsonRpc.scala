package qmf.poc.service.jsonrpc

import qmf.poc.service.agent.*
import qmf.poc.service.catalog.CatalogSnapshot
import qmf.poc.service.messages.{
  ImAlive,
  ImPong,
  ImRunObjectResult,
  ImSnapshot,
  IncomingMessage,
  OmPing,
  OmRequestRunObject,
  OmRequestSnapshot,
  OutgoingMessage,
  OutgoingMessageError,
  OutgoingMessageNotFoundError,
  OutgoingMessagesStorage
}
import zio.json.ast.Json
import zio.json.given
import zio.{IO, ZIO}

/** Finds a corresponding request by its id and check if it is of expected type T
  */
private def correspondingOutgoingMessage(obj: Json.Obj): ZIO[OutgoingMessagesStorage, JsonRPCError, OutgoingMessage] =
  ZIO.serviceWithZIO[OutgoingMessagesStorage] { store =>
    obj.get("id") match
      case Some(jsonId) =>
        jsonId match
          case Json.Num(idNum) =>
            store
              .pop(idNum.intValue())
              .mapError {
                case _: OutgoingMessageNotFoundError =>
                  JsonRPCError(s"There's no message corresponding to id=$idNum")
                case err @ _ =>
                  JsonRPCError(s"Unknown error while looking for message with id=$idNum: $err")
              }
          case n @ _ => ZIO.fail(JsonRPCError(s"`id` expected to be int, but $n found"))
      case None => ZIO.fail(JsonRPCError("Expected `id` not found"))
  }

private def paramsString(obj: Json.Obj): IO[JsonRPCError, String] =
  ZIO.fromOption(obj.get("params").flatMap(_.asString)).orElseFail(JsonRPCError("Params not found"))

private def paramsObj(obj: Json.Obj): IO[JsonRPCError, Json.Obj] =
  ZIO.fromOption(obj.get("params").flatMap(_.asObject)).orElseFail(JsonRPCError("Params not found or not object"))

private def handleMethod(methodJson: Json, obj: Json.Obj): ZIO[Any, OutgoingMessageError, IncomingMessage] =
  (for {
    method <- ZIO.fromOption(methodJson.asString).orElseFail(JsonRPCError("Method is not string"))
    _ <- ZIO.logDebug(s"handle method: $method")
    message <- method match {
      case "alive" =>
        for {
          param <- paramsObj(obj)
          agentId <- paramsString(obj).map(AgentId(_))
          alive <- ZIO.succeed(ImAlive(agentId))
          _ <- ZIO.logDebug(s"alive: $agentId")
        } yield alive
      case _ =>
        ZIO.logWarning(s"Unknown method $method") *> ZIO.fail(JsonRPCError("Unknown method $method"))
    }
  } yield (message)).mapError(jsonError => OutgoingMessageError(jsonError.message, None))

private def handleJsonRPCResult(
    resultObj: Json,
    obj: Json.Obj
): ZIO[OutgoingMessagesStorage, OutgoingMessageError, IncomingMessage] =
  for {
    _ <- ZIO.logDebug(s"handle result")
    request <- correspondingOutgoingMessage(obj).mapError(error => OutgoingMessageError(error.message, None))
    _ <- ZIO.logDebug(s"found request $request")
    message <- (request match {
      // TODO:
      case ping: OmPing =>
        for {
          payload: String <- ZIO.fromOption(resultObj.asString).orElseFail(JsonRPCError("result is not string"))
          pong <- ZIO.succeed(ImPong(payload, ping))
        } yield pong
      case requestSnapshot: OmRequestSnapshot =>
        for {
          result: Json.Obj <- ZIO.fromOption[Json.Obj](resultObj.asObject).orElseFail(JsonRPCError("result must be a object"))
          catalogJson: Json <- ZIO.fromOption(result.get("catalog")).orElseFail(JsonRPCError("result must have catalog"))
          catalog: CatalogSnapshot <- ZIO.fromEither(catalogJson.as[CatalogSnapshot]).mapError(JsonRPCError(_))
          snapshot <- ZIO.succeed(ImSnapshot(catalog, requestSnapshot))
        } yield snapshot
      case requestRunObject: OmRequestRunObject =>
        for {
          ro <- ZIO.fromOption(resultObj.asObject).orElseFail(JsonRPCError("result is not object"))
          body <- ZIO.fromOption(ro.get("body").flatMap(b => b.asString)).orElseFail(JsonRPCError("result has no body"))
          result <- ZIO.succeed(ImRunObjectResult("html", body, requestRunObject))
        } yield result
    }).mapError(error => OutgoingMessageError(error.message, Some(request)))
  } yield message

private def extractError(om: OutgoingMessage, resultObj: Json): IO[JsonRPCError, Nothing] =
  resultObj.asObject match {
    case Some(obj) =>
      val code = obj.get("code") match
        case Some(Json.Num(code)) => code
        case _                    => -1
      val message = obj.get("message") match
        case Some(Json.Str(message)) => s""""$message""""
        case _                       => "(no message)"
      val data = obj.get("data") match
        case Some(Json.Obj(data)) => data
        case _                    => Json.Obj()
      ZIO.fail(JsonRPCError(s"Error code=$code, message=$message, data=$data. ($om)"))
    case None => ZIO.fail(JsonRPCError("Parameter error is not object"))
  }

  // val code = resultObj.asObject
  // ZIO.fail(JsonRPCDecodeError(""))

private def handleError(
    resultObj: Json,
    obj: Json.Obj
): ZIO[OutgoingMessagesStorage, OutgoingMessageError, IncomingMessage] =
  for {
    _ <- ZIO.logDebug(s"handle error")
    request <- correspondingOutgoingMessage(obj).mapError(error => OutgoingMessageError(error.message, None))
    _ <- ZIO.logDebug(s"found request $request")
    result <- extractError(request, resultObj).mapError(error => OutgoingMessageError(error.message, Some(request)))
  } yield result

def fromJsonRpc(message: String): ZIO[OutgoingMessagesStorage, OutgoingMessageError, IncomingMessage] =
  for {
    obj <- ZIO.fromEither(message.fromJson[Json.Obj]).mapError(error => OutgoingMessageError(error, None))
    incoming <- (obj.get("method"), obj.get("result"), obj.get("error")) match {
      case (Some(methodObj), _, _) => handleMethod(methodObj, obj)
      case (_, Some(resultObj), _) => handleJsonRPCResult(resultObj, obj)
      case (_, _, Some(errorObj))  => handleError(errorObj, obj)
      case _                       => ZIO.fail(OutgoingMessageError("Neither method nor result nor error found found", None))
    }
  } yield incoming
