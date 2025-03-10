package qmf.poc.service.http.handlers.ws

import qmf.poc.service.http.handlers.ws.IncomingMessage.given
import zio.http.ChannelEvent.Read
import zio.http.ChannelEvent.UserEvent.HandshakeComplete
import zio.http.{ChannelEvent, Handler, WebSocketApp, WebSocketFrame}
import zio.json.given
import zio.{Ref, ZIO}

import scala.language.postfixOps

def handleIncomingMessage(frameAccumulator: Ref[Array[Byte]], broker: Broker): ZIO[Any, Throwable, Unit] =
  frameAccumulator
    .getAndSet(Array[Byte]())
    .flatMap(accumulated =>
      String(accumulated, "ASCII")
        .fromJson[IncomingMessage]
        .fold(
          error => ZIO.logWarning(s"Parse error: $error (length: ${accumulated.length})"),
          message => ZIO.logDebug(s"Send message $message to broker") *> broker.handle(message).mapError(_.asInstanceOf[Throwable])
        )
    )

def agentWebsocketApp: WebSocketApp[Broker] =
  Handler.webSocket { channel =>
    for {
      // listen for outgoing messages
      broker <- ZIO.service[Broker]
      listener <- (for {
        _ <- ZIO.logDebug(s"ws taking from broker...")
        message <- broker.take
        _ <- ZIO.logDebug(s"ws took from broker $message")
        r <- channel
          .send(Read(WebSocketFrame.Text(message.jsonrpc)))
        _ <- ZIO.logDebug(s"ws send result $message")
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
          (event match
            case HandshakeComplete => broker.put(RequestSnapshot.default)
            case _                 => ZIO.unit
          ).as(ZIO.logDebug(s"ws <== UserEventTriggered $event"))
        case ChannelEvent.Registered =>
          ZIO.logDebug("ws <== Registered")
        case ChannelEvent.Unregistered =>
          ZIO.logDebug("ws <== Unregistered")
        case null =>
          ZIO.logDebug("ws <== Null")
      }
      _ <- listener.interrupt
      _ <- ZIO.logDebug("exit channel")
    } yield ()
  }
