package qmf.poc.service.http.handlers.ws

import qmf.poc.service.http.handlers.ws.IncomingMessage.given
import zio.http.ChannelEvent.Read
import zio.http.{ChannelEvent, Handler, WebSocketApp, WebSocketFrame}
import zio.json.given
import zio.{Ref, Task, ZIO}

def handleIncomingMessage(frameAccumulator: Ref[Array[Byte]], broker: Broker) =
  frameAccumulator
    .getAndSet(Array[Byte]())
    .flatMap(accumulated =>
      String(accumulated, "ASCII")
        .fromJson[IncomingMessage]
        .fold(
          error => ZIO.logWarning(s"Parse error: $error (length: ${accumulated.length})"),
          message => ZIO.logDebug(s"Send message $message to broker") *> broker.handle(message)
        )
    )

def agentWebsocketApp: WebSocketApp[Broker] =
  Handler.webSocket { channel =>
    for
      // listen for outgoing messages
      broker <- ZIO.service[Broker]
      _ <- broker.take
        .flatMap { message =>
          channel.send(Read(WebSocketFrame.Text(message.jsonrpc))) *> ZIO.logDebug(s"ws ==> $message")
        }
        .forever
        .fork
      frameAccumulator <- Ref.make(Array[Byte]())
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
        case ChannelEvent.ExceptionCaught(cause) =>
          ZIO.logError(s"ws <== $cause")
        case ChannelEvent.UserEventTriggered(event) =>
          ZIO.logDebug(s"ws <== $event")
        case ChannelEvent.Registered =>
          ZIO.logDebug("ws <== Registered")
        case ChannelEvent.Unregistered =>
          ZIO.logDebug("ws <== Unregistered")
      }
    yield ()
  }
