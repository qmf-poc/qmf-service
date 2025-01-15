package qmf.poc.service.http

import qmf.poc.service.http.HttpSpec.test
import qmf.poc.service.http.handlers.rest.ping
import qmf.poc.service.http.handlers.ws.echo
import zio.http.*
import zio.http.ChannelEvent.{Read, UserEvent, UserEventTriggered}
import zio.http.Method.GET
import zio.test.TestAspect.ignore
import zio.test.{Spec, TestEnvironment, ZIOSpecDefault, assertTrue}
import zio.{Promise, Scope, ZIO, ZLayer}

object HttpSpec extends ZIOSpecDefault:
  def spec = suite("HTTP test")(
    test("rest should return pong") {
      (for {
        _ <- Server
          .serve(Routes(GET / "ping" -> handler(ping)))
          .provide(Server.live, ZLayer.succeed(Server.Config.default.port(8080)))
          .fork
        response <- ZClient.batched(Request.get(url"http://localhost:8080/ping"))
        body <- response.body.asString
      } yield assertTrue(body == "pong")).provide(Client.default)
    },
    test("ws should echo") {
      def wsApp(lock: Promise[Nothing, Unit]): WebSocketApp[Any] =
        Handler
          .webSocket { channel =>
            channel.receiveAll {
              // Send a "foo" message to the server once the connection is established
              case UserEventTriggered(UserEvent.HandshakeComplete) =>
                channel.send(Read(WebSocketFrame.text("FOO")))
              case Read(WebSocketFrame.Text("FOO")) =>
                channel.send(Read(WebSocketFrame.text("BAR")))
              case Read(WebSocketFrame.Text("BAR")) =>
                ZIO.succeed(println("Goodbye!")) *> lock.succeed(())//channel.send(Read(WebSocketFrame.close(100)))
              case _ =>
                ZIO.unit
            }
          }
      (for {
        _ <- Server
          .serve(Routes(GET / "echo" -> handler(echo.toResponse)))
          .provide(Server.live, ZLayer.succeed(Server.Config.default.port(8080)))
          .fork
        lock <- Promise.make[Nothing, Unit]
        response <-  ZIO.scoped(wsApp(lock).connect("ws://localhost:8080/echo"))
        body <- response.body.asString
        _ <- lock.await
      } yield assertTrue(body == "pong")).provide(Client.default)
    } @@ ignore,
  )
