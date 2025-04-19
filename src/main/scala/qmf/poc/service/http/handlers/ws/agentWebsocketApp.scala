package qmf.poc.service.http.handlers.ws

import qmf.poc.service.agent.{
  Agent,
  AgentId,
  AgentsRegistry
}
import qmf.poc.service.jsonrpc.{JsonRPCError, fromJsonRpc, toJsonRpc}
import zio.http.ChannelEvent.Read
import zio.http.ChannelEvent.UserEvent.HandshakeComplete
import zio.http.{ChannelEvent, Handler, WebSocketApp, WebSocketChannel, WebSocketFrame}
import zio.json.JsonEncoder
import zio.{Ref, ZIO}
import qmf.poc.service.messages.MessageJson.given
import qmf.poc.service.messages.{ImAlive, IncomingMessage, IncomingMessageHandler, OutgoingMessage, OutgoingMessageError, OutgoingMessageHandler, OutgoingMessageIdGenerator, OutgoingMessagesStorage}

import scala.language.postfixOps

def handleIncomingMessage(
    refAgentId: Ref[Option[AgentId]],
    channel: WebSocketChannel,
    frameAccumulator: Ref[Array[Byte]]
): ZIO[OutgoingMessagesStorage & IncomingMessageHandler & AgentsRegistry, Nothing, Unit] =
  for {
    handler <- ZIO.service[IncomingMessageHandler]
    accumulated <- frameAccumulator.getAndSet(Array[Byte]())
    asciiStr = String(accumulated) // TODO: encoding
    _ <- ZIO.logDebug(s"handleIncomingMessage(raw): ${ellipse(asciiStr)}")
    incomingMessage <- fromJsonRpc(asciiStr)
      .foldZIO(
        { error =>
          ZIO.logWarning(error.toString) *> handler.handle(error)
        },
        msg =>
          ZIO.logDebug(s"handleIncomingMessage(decoded): $msg") *> (msg match {
            case ImAlive(agentId) =>
              ZIO.logDebug(s"register agent $agentId") *>
                refAgentId.set(Some(agentId)) *>
                ZIO.serviceWithZIO[AgentsRegistry](
                  _.registerAgent(
                    agentId,
                    (message: OutgoingMessage) => {
                      ZIO.logDebug(s"agent $agentId send $message") *>
                        toJsonRpc(message).flatMap(jsonRpc => channel.send(Read(WebSocketFrame.Text(jsonRpc, true))))
                    }
                  )
                )
            case _ => handler.handle(msg)
          })
      )
      .catchAll { e => ZIO.logWarning(e.toString) *> handler.handle(OutgoingMessageError("Server error", None)) }
  } yield ()

private def ellipse(s: String): String = if (s.length > 200) s.take(200) + "..." else s

def agentWebsocketApp(using
    JsonEncoder[OutgoingMessage]
): WebSocketApp[OutgoingMessagesStorage & IncomingMessageHandler & AgentsRegistry] =
  Handler.webSocket { channel =>
    for {
      /*
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
       */
      frameAccumulator <- Ref.make(Array[Byte]())
      refAgentId <- Ref.make[Option[AgentId]](None)
      _ <- ZIO.logDebug(s"ws channel.receiveAll")
      // listen for incoming messages
      _ <- channel
        .receiveAll {
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
                  _ <- ZIO.when(frame.isFinal)(handleIncomingMessage(refAgentId, channel, frameAccumulator))
                } yield ()
              case frame: WebSocketFrame.Continuation =>
                for
                  _ <- ZIO.logDebug(s"ws <== WebSocketFrame.Continuation(${frame.buffer.length})")
                  _ <- frameAccumulator.update(_ ++ frame.buffer)
                  _ <- ZIO.when(frame.isFinal)(handleIncomingMessage(refAgentId, channel, frameAccumulator))
                yield ()
              case _ =>
                ZIO.logDebug("Unknown")
          case ChannelEvent.ExceptionCaught(cause) =>
            ZIO.logError(s"ws <== $cause")
          case ChannelEvent.UserEventTriggered(event) =>
            ZIO.logDebug(s"ws <== UserEventTriggered($event)")
          /*
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
        .ensuring {
          refAgentId.get.flatMap {
            case Some(agentId) =>
              ZIO.logDebug(s"unrgister agent $agentId") *> ZIO.serviceWithZIO[AgentsRegistry](_.unregisterAgent(agentId))
            case None => ZIO.unit
          }
        }
      _ <- ZIO.logDebug("exit channel")
    } yield () // ).mapError(e => Exception(e.toString))
  }
