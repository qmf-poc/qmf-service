package qmf.poc.service.http.rest

import zio.{Promise, ZIO, ZLayer, given}
import zio.http.{Client, Request, Routes, Server, ZClient, url}
import zio.test.{Spec, ZIOSpecDefault, assertCompletes, assertTrue}

object PingSpec extends ZIOSpecDefault:
  def spec: Spec[Any, Throwable] = suite("Ping test")(
    test("should return pong") {
      (for {
        started <- Promise.make[Nothing, Unit]
        fiber <- Server
          .install(Routes(ping))
          .zipRight(started.succeed(()))
          .zipRight(ZIO.never)
          .provide(
            Server.live,
            ZLayer.succeed(Server.Config.default.port(8080)),
          )
          .fork
        _ <- started.await
        _ <- fiber.interrupt.delay(1.seconds).fork
        response <- ZClient.batched(Request.get(url"http://localhost:8080/ping"))
        body <- response.body.asString
      } yield assertTrue(body == "pong")).provide(Client.default)
    }
  )
