package qmf.poc.service.zio

import qmf.poc.service.zio.ZIOSpec.test
import zio.ZIO
import zio.test.*
import zio.test.TestServices.test
var sideEffect1 = false
var sideEffect2 = false
var sideEffect3 = false
var sideEffectCounter = 0

def setEffect1(): Boolean =
  sideEffect1 = true
  true

def zioLambda = ZIO.succeed(setEffect1())

def retZIO = {
  println("enter retZIO")
  sideEffect2 = true
  ZIO.succeed(setEffect1())
}

def incCounter(): Int =
  sideEffectCounter += 1
  sideEffectCounter

object ZIOSpec extends ZIOSpecDefault:
  def spec = suite("ZIO tests")(
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
    },
    test("counters by modifier") {
      def modify() = ZIO.succeed(incCounter())
      val c0 = sideEffectCounter
      val v = for {
        c1 <- modify()
        c2 <- modify()
      } yield (c1, c2)
      val cc = sideEffectCounter
      assertZIO(v)(Assertion.equalTo((1, 2))) && assertTrue(c0 == 0, cc == 0)
    },
    test("counters by value") {
      def modify() = ZIO.succeed(incCounter())
      val modifier = modify()
      val c0 = sideEffectCounter
      val v = for {
        c1 <- modifier
        c2 <- modifier
      } yield (c1, c2)
      val cc = sideEffectCounter
      assertZIO(v)(Assertion.equalTo((1, 2))) && assertTrue(c0 == 0, cc == 0)
    },
    test("zio from withing blocker") {
      var side = List[Int]()
      def sideEffect(i: Int): Unit = {
        println(s"within sideEffect $i")
        side = i :: side
      }
      def someZIO(i: Int) = ZIO.succeed(sideEffect(i))
      val s0 = side.map(e => e)
      println(s"before zioInCode $side")
      def zioInCode() = ZIO.attempt {
        List(1, 2, 3).foreach { el =>
          println(s"sideEffect($el)")
          sideEffect(el)
          ZIO.attempt {
            println(s"zio.print.sideEffect(${el * 10})")
            sideEffect(el * 10)
            ZIO.debug(s"zio.sideEffect(${el * 10})")
          }
        }
        side
      }
      println(s"after zioInCode $side")
      val ss = side.map(e => e)
      println("before for")
      val v = for {
        _ <- ZIO.debug("for in")
        _ <- zioInCode()
        _ <- ZIO.debug("for out")
      } yield (side)
      println("after for")
      assertZIO(v)(Assertion.equalTo(List(3, 2, 1))) && assertTrue(s0.isEmpty, ss.isEmpty)
    },
    test("for = syntax") {
      for {
        a <- ZIO.succeed(1)
        b <- ZIO.succeed(2)
        c = List(a, b)
        d <- ZIO.succeed(c.map(e => e * 10))
      } yield assert(d)(Assertion.equalTo(List(10, 20)))
    },
    test("for = syntax 2") {
      val xx = for {
        _ <- ZIO.unit
        a = 1
        b = 2
        c = List(a, b)
        d <- ZIO.succeed(c.map(e => e * 10))
        x = d
      } yield x
      assertZIO(xx)(Assertion.equalTo(List(10, 20)))
    },
    test("flatmap syntax 2") {
      val xx = ZIO.unit *> {
        val a = 1
        val b = 2
        val c = List(a, b)
        val x = ZIO.succeed(c.map(e => e * 10)).map { d =>
          d
        }
        x
      }
      assertZIO(xx)(Assertion.equalTo(List(10, 20)))
    },
    test("zio within pure") {
      def f = ZIO.succeed{
        var l = List(1,2)
        def ff() = {
          l = List(3,4)
        }
        val foo = ZIO.succeed{ff()}.flatMap(x => ZIO.succeed(x))
        l
      }
      assertZIO(f)(Assertion.equalTo(List(3, 4)))
    },
  )
