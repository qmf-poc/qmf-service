package qmf.poc.service.http.handlers.ws

import zio.ZIO
import zio.http.ChannelEvent.Read
import zio.http.Method.GET
import zio.http.{Handler, WebSocketApp, WebSocketFrame, handler}

def echo: WebSocketApp[Any] =
  Handler.webSocket { channel =>
    channel.receiveAll {
      case Read(WebSocketFrame.Text("FOO")) =>
        channel.send(Read(WebSocketFrame.Text("BAR")))
      case Read(WebSocketFrame.Text("BAR")) =>
        channel.send(Read(WebSocketFrame.Text("FOO")))
      case Read(WebSocketFrame.Text(text)) =>
        channel.send(Read(WebSocketFrame.Text(text))).repeatN(10)
      case _ =>
        ZIO.unit
    }
  }
