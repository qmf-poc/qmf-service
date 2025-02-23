package qmf.poc.service.zio

import qmf.poc.service.zio.ZIOSpec.test
import zio.ZIO
import zio.test.*
var sideEffect1 = false
var sideEffect2 = false
var sideEffect3 = false

def setEffect1(): Boolean =
  sideEffect1 = true
  true

def zioLambda = ZIO.succeed(setEffect1())

def retZIO = {
  println("enter retZIO")
  sideEffect2 = true
  ZIO.succeed(setEffect1())
}

object ZIOSpec extends ZIOSpecDefault:
  def spec: Spec[Any, Nothing] = suite("ZIO tests")(
    test("succeed") {
      val v = for {
        ebefore <- ZIO.succeed(sideEffect1)
        z <- zioLambda
        eafter <- ZIO.succeed(sideEffect1)
      } yield (ebefore, eafter)
      val e = sideEffect1
      assertZIO(v)(Assertion.equalTo((false, true))) && assertTrue(!e, sideEffect1)
    },
    test("return") {
      val v = for {
        _ <- ZIO.succeed(println("enter for"))
        ebefore <- ZIO.succeed(sideEffect2)
        z <- retZIO
        eafter <- ZIO.succeed(sideEffect2)
      } yield (ebefore, eafter)
      println("after for")
      val e = sideEffect2
      assertZIO(v)(Assertion.equalTo((false, true))) && assertTrue(!e, sideEffect2)
    }
  )
