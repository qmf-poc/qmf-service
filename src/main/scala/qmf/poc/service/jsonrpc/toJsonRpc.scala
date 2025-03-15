package qmf.poc.service.jsonrpc

import qmf.poc.service.agent.{OutgoingMessage, Ping, RequestRunObject, RequestSnapshot}
import zio.{URIO, ZIO}
import zio.json.{EncoderOps, JsonEncoder}

private def jsonRpcRequest(method: String, message: OutgoingMessage)(using
    JsonEncoder[OutgoingMessage]
): URIO[JsonRpcOutgoingMessagesStore, String] =
  ZIO.serviceWithZIO[JsonRpcOutgoingMessagesStore] { store =>
    ZIO.succeed(s"""{"jsonrpc": "2.0", "id": ${store.push(message)}, "method": "$method", "params": ${message.toJson}}""")
  }

def toJsonRpc(message: OutgoingMessage)(using JsonEncoder[OutgoingMessage]): URIO[JsonRpcOutgoingMessagesStore, String] =
  message match
    case ping @ Ping(_)                            => jsonRpcRequest("ping", ping)
    case request @ RequestSnapshot(_, _)           => jsonRpcRequest("snapshot", request)
    case request @ RequestRunObject(_, _, _, _, _) => jsonRpcRequest("run", request)
