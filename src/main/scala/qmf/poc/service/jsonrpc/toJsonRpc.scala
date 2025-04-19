package qmf.poc.service.jsonrpc

import qmf.poc.service.messages.{OmPing, OmRequestRunObject, OmRequestSnapshot, OutgoingMessage, OutgoingMessagesStorage}
import zio.{URIO, ZIO}
import zio.json.{EncoderOps, JsonEncoder}

private def jsonRpcRequest(method: String, message: OutgoingMessage)(using
    JsonEncoder[OutgoingMessage]
): URIO[OutgoingMessagesStorage, String] =
  ZIO.serviceWithZIO[OutgoingMessagesStorage] { store =>
    ZIO.succeed(s"""{"jsonrpc": "2.0", "id": ${store.push(message)}, "method": "$method", "params": ${message.toJson}}""")
  }

def toJsonRpc(message: OutgoingMessage)(using JsonEncoder[OutgoingMessage]): URIO[OutgoingMessagesStorage, String] =
  message match
    case ping @ OmPing(_, _)                            => jsonRpcRequest("ping", ping)
    case request @ OmRequestSnapshot(_, _, _)           => jsonRpcRequest("snapshot", request)
    case request @ OmRequestRunObject(_, _, _, _, _, _, _) => jsonRpcRequest("run", request)
