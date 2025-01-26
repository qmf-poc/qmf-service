package qmf.poc.service.http.handlers.ws

import zio.Console.printLine
import zio.ZIO
import zio.http.ChannelEvent.Read
import zio.http.{Handler, WebSocketApp, WebSocketFrame}
import zio.json.ast.{Json, JsonCursor}
import zio.json.given

//case class JSONRPC(method: String, params: String)

object JSONRPC:
  def parse(jsonString: String): Option[(String, String | java.math.BigDecimal)] =
    val methodC = JsonCursor.field("method")
    val paramsC = JsonCursor.field("params")
    jsonString.fromJson[Json].toOption.flatMap { json =>
      for {
        method <- json.get(methodC).toOption.collect { case Json.Str(s) => s }
        params <- json.get(paramsC).toOption.collect {
          case Json.Str(s) => s
          case Json.Num(dec) => dec
        }
      } yield (method, params)
    }


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
        case Read(WebSocketFrame.Text(frame)) => ZIO.logDebug(s"ws <== $frame") *> JSONRPC.parse(frame).collect {
            case ("ping", payload: String) => Ping(payload)
            case ("alive", agent: String) => Alive(agent)
          }.fold(ZIO.logWarning(s"Fail parse: $frame").unit)
          (broker.handle)
        case _ => ZIO.unit
      }
    yield ()
  }
