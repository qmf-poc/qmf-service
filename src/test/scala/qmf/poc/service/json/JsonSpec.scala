package qmf.poc.service.json

import qmf.poc.service.http.handlers.ws.JSONRPC
import zio.test.{Assertion, Spec, ZIOSpecDefault, assert}

object JsonSpec extends ZIOSpecDefault:
  def spec: Spec[Any, Nothing] = suite("Json test")(
    test("match ping"){
      val string = """{"jsonrpc": "2.0", "method": "ping", "params": "agent"}"""
      val parsed = JSONRPC.parse(string)
      assert(parsed)(Assertion.equalTo(Some("ping", "agent")))
    },
    test("match ping payload"){
      val string = """{"jsonrpc": "2.0", "method": "ping", "params": "agent"}"""
      val m = JSONRPC.parse(string).collect {
        case ("ping", payload: String) => true
        case _ => false
      }.fold(false)(_=>true)
      assert(m)(Assertion.isTrue)
    },
  )
