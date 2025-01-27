package qmf.poc.service.http.handlers.ws

import qmf.poc.service.catalog.CatalogSnapshot
import zio.Console.printLine
import zio.ZIO
import zio.http.ChannelEvent.Read
import zio.http.{Handler, WebSocketApp, WebSocketFrame}
//import zio.json.ast.{Json, JsonCursor}
import IncomingMessage.given
import zio.json.given

//case class JSONRPC(method: String, params: String)
/*
object JSONRPC:
  def parse(jsonString: String): Option[(String, String | java.math.BigDecimal|CatalogSnapshot)] =
    val methodC = JsonCursor.field("method")
    val paramsC = JsonCursor.field("params")
    jsonString.fromJson[Json].toOption.flatMap { json =>
      for {
        method <- json.get(methodC).toOption.collect { case Json.Str(s) => s }
        params <- json.get(paramsC).toOption.collect {
          case Json.Str(s) => s
          case Json.Num(dec) => dec
          case Json.Obj(o) if (method == "snapshot") =>
            o.toString
        }
      } yield (method, params)
    }

*/
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
        /*
          {
          frame.fromJson[IncomingMessage] match
            case Right(message) => broker.handle(message)
            case Left(error) => ZIO.logWarning(s"Incoming message parse error. $error: $frame")
        }

         */
        case _ => ZIO.unit
      }
    yield ()
  }
