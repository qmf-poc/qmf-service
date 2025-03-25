package qmf.poc.service.http.handlers.ws

import qmf.poc.service.agent.{AgentError, Broker, OutgoingMessage, OutgoingMessageIdGenerator}
import qmf.poc.service.jsonrpc.{JsonRPCError, JsonRpcOutgoingMessagesStore, fromJsonRpc, toJsonRpc}
import zio.http.ChannelEvent.Read
import zio.http.{ChannelEvent, Handler, WebSocketApp, WebSocketFrame}
import zio.json.JsonEncoder
import zio.{Ref, ZIO}

import scala.language.postfixOps

def handleIncomingMessage(
    frameAccumulator: Ref[Array[Byte]]
): ZIO[JsonRpcOutgoingMessagesStore & OutgoingMessageIdGenerator & Broker, Nothing, Unit] =
  for {
    broker <- ZIO.service[Broker]
    accumulated <- frameAccumulator.getAndSet(Array[Byte]())
    asciiStr = String(accumulated, "ASCII")
    _ <- ZIO.logDebug(s"handleIncomingMessage(raw): ${ellipse(asciiStr)}")
    incomingMessage <- fromJsonRpc(String(accumulated, "ASCII"))
      .foldZIO(
        {
          case e: AgentError   => ZIO.logWarning(e.toString) *> broker.handle(e)
          case e: JsonRPCError => ZIO.logWarning(e.toString) *> broker.handle(AgentError(e.message, None))
        },
        msg =>
          ZIO.logDebug(s"handleIncomingMessage(decoded): $msg") *> broker
            .handle(msg)
      )
      .catchAll { e => ZIO.logWarning(e.toString) *> broker.handle(AgentError("Server error", None)) }
  } yield ()

private def ellipse(s: String): String = if (s.length > 200) s.take(200) + "..." else s

def agentWebsocketApp(using
    JsonEncoder[OutgoingMessage]
): WebSocketApp[Broker & JsonRpcOutgoingMessagesStore & OutgoingMessageIdGenerator] =
  Handler.webSocket { channel =>
    for {
      // listen for outgoing messages
      broker <- ZIO.service[Broker]
      listener <- (for {
        _ <- ZIO.logDebug(s"ws taking from broker...")
        message <- broker.take
        _ <- ZIO.logDebug(s"ws took from broker $message")
        jsonRpc <- toJsonRpc(message)
        _ <- ZIO.logDebug(s"ws send $jsonRpc")
        r <- channel.send(Read(WebSocketFrame.Text(jsonRpc)))
      } yield ()).forever.ensuring(ZIO.logDebug("ws broker listener interrupted")).fork
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
                _ <- ZIO.logDebug(s"ws <== WebSocketFrame.Text(${ellipse(frame.text)}), final = ${frame.isFinal}")
                _ <- frameAccumulator.update(_ ++ frame.text.getBytes)
                _ <- ZIO.when(frame.isFinal)(handleIncomingMessage(frameAccumulator))
              } yield ()
            case frame: WebSocketFrame.Continuation =>
              for
                _ <- ZIO.logDebug(s"ws <== WebSocketFrame.Continuation(${frame.buffer.length})")
                _ <- frameAccumulator.update(_ ++ frame.buffer)
                _ <- ZIO.when(frame.isFinal)(handleIncomingMessage(frameAccumulator))
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
    } yield () // ).mapError(e => Exception(e.toString))
  }
