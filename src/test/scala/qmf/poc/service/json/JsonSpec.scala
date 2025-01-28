package qmf.poc.service.json

import qmf.poc.service.catalog.{CatalogSnapshot, ObjectDirectory}
import qmf.poc.service.http.handlers.ws.*
import zio.json.{JsonDecoder, given}
import zio.test.{Assertion, Spec, TestAspect, ZIOSpecDefault, assert}
import IncomingMessage.given
import zio.Console.printLine

object JsonSpec extends ZIOSpecDefault:
  def spec: Spec[Any, Nothing] = suite("Json test")(
    test("for 1") {
      val v = None.toRight("b")
      assert(v)(Assertion.equalTo(Left("b")))
    },
    test("for 11") {
      val v = None.toRight("left").flatMap{(a: String) =>  Left(a + "1")}
      assert(v)(Assertion.equalTo(Left("left")))
    },
    test("for 12") {
      val v = Some("a").toRight("left").flatMap{(a: String) =>  Left(a + "1")}
      assert(v)(Assertion.equalTo(Left("a1")))
    },
    test("for 2") {
      val v = Some("a").toRight("b")
      assert(v)(Assertion.equalTo(Right("a")))
    },
    test("for 3") {
      val v = Some("a").toLeft("b")
      assert(v)(Assertion.equalTo(Left("a")))
    },
    test("match jsonrpc error method not found") {
      val string = """{"jsonrpc": "2.0", "params": "agent"}"""
      val parsed = string.fromJson[IncomingMessage]
      assert(parsed)(Assertion.equalTo(Left("(Method not found)")))
    },
    test("match jsonrpc ping") {
      val string = """{"jsonrpc": "2.0", "method": "ping", "params": "agent"}"""
      val parsed = string.fromJson[IncomingMessage]
      assert(parsed)(Assertion.equalTo(Right(Ping("agent"))))
    },
    test("json objectDirectory"){
      val string ="""{"lastUsed":"2025-01-22 21:47:55.478313","model":"2025-01-09 10:46:01.929","modified":"2025-01-09 10:46:02.285706","name":"EMPLOYEE_TO_REPOSITORY","objectLevel":2,"owner":"DB2INST1","restricted":"N","subType":"SQL     ","type":"QUERY   "}"""
      val parsed = string.fromJson[ObjectDirectory]
      assert(parsed)(Assertion.equalTo(Right(Ping("agent"))))
    },
    test("json objectDirectories"){
      val string ="""[{"lastUsed":"2025-01-22 21:47:55.478313","model":"2025-01-09 10:46:01.929","modified":"2025-01-09 10:46:02.285706","name":"EMPLOYEE_TO_REPOSITORY","objectLevel":2,"owner":"DB2INST1","restricted":"N","subType":"SQL     ","type":"QUERY   "},{"lastUsed":"2025-01-09 10:46:47.543515","model":"2025-01-09 10:46:47.028","modified":"2025-01-09 10:46:47.390611","name":"ORG_TO_QMF","objectLevel":2,"owner":"DB2INST1","restricted":"N","subType":"SQL     ","type":"QUERY   "}]"""
      val parsed = string.fromJson[Seq[ObjectDirectory]]
      assert(parsed)(Assertion.equalTo(Right(Ping("agent"))))
    },
    test("match jsonrpc snapshot"){
      val string ="""{"jsonrpc": "2.0", "method": "snapshot", "params": {"objectData":[{"name":"EMPLOYEE_TO_REPOSITORY","owner":"DB2INST1","seq":1,"type":"QUERY   "},{"name":"EMPLOYEE_TO_REPOSITORY","owner":"DB2INST1","seq":2,"type":"QUERY   "},{"name":"ORG_TO_QMF","owner":"DB2INST1","seq":1,"type":"QUERY   "},{"name":"ORG_TO_QMF","owner":"DB2INST1","seq":2,"type":"QUERY   "}],"objectDirectories":[{"created":"2025-01-09 10:46:01.929","lastUser":"2025-01-22 21:47:55.478313","model":"        ","modified":"2025-01-09 10:46:02.285706","name":"EMPLOYEE_TO_REPOSITORY","objectLevel":2,"owner":"DB2INST1","restricted":"N","subType":"SQL     ","type":"QUERY   "},{"created":"2025-01-09 10:46:47.028","lastUser":"2025-01-09 10:46:47.543515","model":"        ","modified":"2025-01-09 10:46:47.390611","name":"ORG_TO_QMF","objectLevel":2,"owner":"DB2INST1","restricted":"N","subType":"SQL     ","type":"QUERY   "}],"objectRemarks":[{"name":"EMPLOYEE_TO_REPOSITORY","owner":"DB2INST1","remarks":"","type":"QUERY   "},{"name":"ORG_TO_QMF","owner":"DB2INST1","remarks":"","type":"QUERY   "}]}}"""
      val parsed = string.fromJson[IncomingMessage]
      assert(parsed)(Assertion.equalTo(Right("")))
    } @@ TestAspect.ignore,
    /*
    test("match ping") {
      val string = """{"jsonrpc": "2.0", "method": "ping", "params": "agent"}"""
      val parsed = JSONRPC.parse(string)
      assert(parsed)(Assertion.equalTo(Some("ping", "agent")))
    },
    test("match ping payload") {
      val string = """{"jsonrpc": "2.0", "method": "ping", "params": "agent"}"""
      val m = JSONRPC.parse(string).collect {
        case ("ping", payload: String) => true
        case _ => false
      }.fold(false)(_ => true)
      assert(m)(Assertion.isTrue)
    },
    test("match catalog") {
      val string = """{"jsonrpc": "2.0", "method": "snapshot", "params": {"objectData":[{"name":"EMPLOYEE_TO_REPOSITORY","owner":"DB2INST1","seq":1},{"name":"EMPLOYEE_TO_REPOSITORY","owner":"DB2INST1","seq":2},{"name":"ORG_TO_QMF","owner":"DB2INST1","seq":1},{"name":"ORG_TO_QMF","owner":"DB2INST1","seq":2}],"objectDirectories":[{"lastUsed":"2025-01-22 21:47:55.478313","model":"2025-01-09 10:46:01.929","modified":"2025-01-09 10:46:02.285706","name":"EMPLOYEE_TO_REPOSITORY","objectLevel":2,"owner":"DB2INST1","restricted":"N","subType":"SQL     ","type":"QUERY   "},{"lastUsed":"2025-01-09 10:46:47.543515","model":"2025-01-09 10:46:47.028","modified":"2025-01-09 10:46:47.390611","name":"ORG_TO_QMF","objectLevel":2,"owner":"DB2INST1","restricted":"N","subType":"SQL     ","type":"QUERY   "}],"objectRemarks":[{"name":"EMPLOYEE_TO_REPOSITORY","owner":"DB2INST1","remarks":"","type":"QUERY   "},{"name":"ORG_TO_QMF","owner":"DB2INST1","remarks":"","type":"QUERY   "}]}}"""
      val m = JSONRPC.parse(string).collect {
        case ("snapshot", payload: String) =>
          println(payload)
          true
        case _ => false
      }.fold(false)(_ => true)
      assert(m)(Assertion.isTrue)
    }*/
  )
