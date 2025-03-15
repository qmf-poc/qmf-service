package qmf.poc.service.http.handlers.ws

import qmf.poc.service.agent.{Broker, OutgoingMessage, OutgoingMessageIdGenerator}
import qmf.poc.service.jsonrpc.{JsonRPCDecodeError, JsonRpcOutgoingMessagesStore, fromJsonRpc, toJsonRpc}
import qmf.poc.service.repository.RepositoryError
import zio.http.ChannelEvent.Read
import zio.http.{ChannelEvent, Handler, WebSocketApp, WebSocketFrame}
import zio.json.JsonEncoder
import zio.{Ref, ZIO}

import scala.language.postfixOps

def handleIncomingMessage(
    frameAccumulator: Ref[Array[Byte]],
    broker: Broker
): ZIO[JsonRpcOutgoingMessagesStore & OutgoingMessageIdGenerator, JsonRPCDecodeError | RepositoryError, Unit] =
  for {
    accumulated <- frameAccumulator.getAndSet(Array[Byte]())
    message <- fromJsonRpc(String(accumulated, "ASCII")).tapError(error =>
      ZIO.logWarning(s"Parse error: ${error.message} (length: ${accumulated.length})")
    )
    _ <- ZIO.logDebug(s"Send message $message to broker")
    r <- broker.handle(message).tapError(error => ZIO.logWarning(s"Handle error: ${error.message}"))
  } yield r

def agentWebsocketApp(using
    JsonEncoder[OutgoingMessage]
): WebSocketApp[Broker & JsonRpcOutgoingMessagesStore & OutgoingMessageIdGenerator] =
  Handler.webSocket { channel =>
    (for {
      // listen for outgoing messages
      broker <- ZIO.service[Broker]
      listener <- (for {
        _ <- ZIO.logDebug(s"ws taking from broker...")
        message <- broker.take
        _ <- ZIO.logDebug(s"ws took from broker $message")
        jsonRpc <- toJsonRpc(message)
        _ <- ZIO.logDebug(s"ws send $jsonRpc")
        r <- channel.send(Read(WebSocketFrame.Text(jsonRpc)))
      } yield ()).forever.fork.ensuring(ZIO.logDebug("ws broker listener interrupted"))
      frameAccumulator <- Ref.make(Array[Byte]())
      _ <- ZIO.logDebug(s"ws channel.receiveAll")
      // listen for incoming messages
      _ <- channel.receiveAll {
        case ChannelEvent.Read(message) =>
          message match
            case frame: WebSocketFrame.Binary =>
              ZIO.logDebug(s"ws <== $frame")
            case WebSocketFrame.Close(status, reason) =>
              ZIO.logDebug(s"ws <== $status($reason)")
            case WebSocketFrame.Pong =>
              ZIO.logDebug(s"ws <== Pong")
            case WebSocketFrame.Ping =>
              ZIO.logDebug(s"ws <== Ping")
            case frame: WebSocketFrame.Text =>
              for {
                _ <- ZIO.logDebug(s"ws <== WebSocketFrame.Text(${frame.text.length})")
                _ <- frameAccumulator.update(_ ++ frame.text.getBytes)
                _ <- ZIO.when(frame.isFinal)(handleIncomingMessage(frameAccumulator, broker))
              } yield ()
            case frame: WebSocketFrame.Continuation =>
              for
                _ <- ZIO.logDebug(s"ws <== WebSocketFrame.Continuation(${frame.buffer.length})")
                _ <- frameAccumulator.update(_ ++ frame.buffer)
                _ <- ZIO.when(frame.isFinal)(handleIncomingMessage(frameAccumulator, broker))
              yield ()
            case _ =>
              ZIO.logDebug("Unknown")
        case ChannelEvent.ExceptionCaught(cause) =>
          ZIO.logError(s"ws <== $cause")
        case ChannelEvent.UserEventTriggered(event) =>
          ZIO.logDebug(s"ws <== UserEventTriggered($event)")
        /*
          TODO: RequestSnapshot on start
          (event match
            case HandshakeComplete => broker.put(RequestSnapshot.default)
            case _                 => ZIO.unit
          ).as(ZIO.logDebug(s"ws <== UserEventTriggered $event"))
         */
        case ChannelEvent.Registered =>
          ZIO.logDebug("ws <== Registered")
        case ChannelEvent.Unregistered =>
          ZIO.logDebug("ws <== Unregistered")
        case null =>
          ZIO.logDebug("ws <== Null")
      }
      _ <- listener.interrupt
      _ <- ZIO.logDebug("exit channel")
    } yield ()).mapError(e => Exception(e.toString))
  }
