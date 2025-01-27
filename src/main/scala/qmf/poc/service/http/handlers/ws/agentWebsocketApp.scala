package qmf.poc.service.http.handlers.ws

import qmf.poc.service.catalog.CatalogSnapshot
import zio.Console.printLine
import zio.ZIO
import zio.http.ChannelEvent.Read
import zio.http.{Handler, WebSocketApp, WebSocketFrame}
import IncomingMessage.given
import zio.json.given

def agentWebsocketApp: WebSocketApp[Broker] =
  Handler.webSocket { channel =>
    for
      // listen for outgoing messages
      broker <- ZIO.service[Broker]
      _ <- broker.take.flatMap { message =>
        channel.send(Read((WebSocketFrame.Text(message.jsonrpc)))) *> ZIO.logDebug(s"ws ==> $message")
      }.forever.fork
      // listen for incoming messages
      _ <- channel.receiveAll {
        case Read(WebSocketFrame.Text(frame)) =>
          ZIO.logDebug(s"ws <== $frame") *>
            frame.fromJson[IncomingMessage].fold(
              error => ZIO.logWarning(s"Incoming message parse error. $error: $frame"),
              broker.handle
            )
        case _ => ZIO.unit
      }
    yield ()
  }
