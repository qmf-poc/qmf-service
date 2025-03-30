package qmf.poc.service.json

import qmf.poc.service.catalog.CatalogSnapshot
import qmf.poc.service.json.JsonSpec.test
import zio.json.ast.Json
import zio.json.{DeriveJsonEncoder, JsonEncoder, given}
import zio.test.{Assertion, Spec, ZIOSpecDefault, assert, assertCompletes}

object JsonSpec extends ZIOSpecDefault:
  def spec: Spec[Any, Nothing] = suite("Json test")(
    test("calculated case class field by derived") {
      case class C(a: Int) {
        val b: String = s"=${a * 2}="
      }
      given JsonEncoder[C] = DeriveJsonEncoder.gen[C]
      assert(C(1).toJson)(Assertion.equalTo("""{"a":1}"""))
      assert(C(1).toJson)(Assertion.not(Assertion.equalTo("""{"a":1,"b":"=2="}""")))
    },
    test("calculated case class field by contrmap") {
      case class C(a: Int) {
        val b: String = s"=${a * 2}="
      }
      given JsonEncoder[C] = JsonEncoder[Map[String, Json]].contramap(c => Map("a" -> Json.Num(c.a), "b" -> Json.Str(c.b)))
      assert(C(1).toJson)(Assertion.equalTo("""{"a":1,"b":"=2="}"""))
    },
    test("for 1") {
      val v = None.toRight("b")
      assert(v)(Assertion.equalTo(Left("b")))
    },
    test("for 11") {
      val v = None.toRight("left").flatMap { (a: String) => Left(a + "1") }
      assert(v)(Assertion.equalTo(Left("left")))
    },
    test("for 12") {
      val v = Some("a").toRight("left").flatMap { (a: String) => Left(a + "1") }
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
    test("CatalogSnapshot encode") {
      // Arrange
      val catalog = CatalogSnapshot(Nil)
      // Act
      val jsonRpc = catalog.toJson
      // Assert
      val expected = """{"objectData":[],"objectRemarks":[],"objectDirectories":[]}"""
      assert(jsonRpc)(Assertion.equalTo(expected))
    },
    test("CatalogSnapshot result encode") {
      // Arrange
      val catalog = CatalogSnapshot(Seq.empty)
      val result = Map("catalog" -> catalog)
      // Act
      val jsonRpc = result.toJson
      // Assert
      val expected = """{"catalog":{"objectData":[],"objectRemarks":[],"objectDirectories":[]}}"""
      assert(jsonRpc)(Assertion.equalTo(expected))
    },
    test("CatalogSnapshot decode") {
      val jsonRpc = """{"objectData":[],"objectRemarks":[],"objectDirectories":[]}"""
      val catalogSnapshot = jsonRpc.fromJson[CatalogSnapshot]
      assert(catalogSnapshot)(Assertion.equalTo(CatalogSnapshot(Nil)))
    },
    test("CatalogSnapshot result decode") {
      val jsonRpc = """{"catalog":{"objectData":[],"objectRemarks":[],"objectDirectories":[]}}"""
      val objJson: Either[String, Json.Obj] = jsonRpc.fromJson[Json.Obj]
      val objCatalog: Either[String, Json] = objJson.toOption.flatMap(obj => obj.get("catalog")).toRight("No catalog")
      val catalog: Either[String, CatalogSnapshot] = objCatalog.flatMap(obj => obj.as[CatalogSnapshot])

      assert(catalog)(Assertion.equalTo(Right(CatalogSnapshot(Nil))))
    }
    /*
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
     */
    /*
    test("json objectDirectory") {
      val string =
        """{"lastUsed":"2025-01-22 21:47:55.478313","model":"2025-01-09 10:46:01.929","modified":"2025-01-09 10:46:02.285706","name":"EMPLOYEE_TO_REPOSITORY","objectLevel":2,"owner":"DB2INST1","restricted":"N","subType":"SQL     ","type":"QUERY   "}"""
      val parsed = string.fromJson[ObjectDirectory]
      assert(parsed)(Assertion.equalTo(Right(Ping("agent"))))
    },
    test("json objectDirectories") {
      val string =
        """[{"lastUsed":"2025-01-22 21:47:55.478313","model":"2025-01-09 10:46:01.929","modified":"2025-01-09 10:46:02.285706","name":"EMPLOYEE_TO_REPOSITORY","objectLevel":2,"owner":"DB2INST1","restricted":"N","subType":"SQL     ","type":"QUERY   "},{"lastUsed":"2025-01-09 10:46:47.543515","model":"2025-01-09 10:46:47.028","modified":"2025-01-09 10:46:47.390611","name":"ORG_TO_QMF","objectLevel":2,"owner":"DB2INST1","restricted":"N","subType":"SQL     ","type":"QUERY   "}]"""
      val parsed = string.fromJson[Seq[ObjectDirectory]]
      assert(parsed)(Assertion.equalTo(Right(Ping("agent"))))
    }
     */
    /*
    test("match jsonrpc snapshot") {
      val string =
        """{"jsonrpc": "2.0", "method": "snapshot", "params": {"objectData":[{"name":"EMPLOYEE_TO_REPOSITORY","owner":"DB2INST1","seq":1,"type":"QUERY   "},{"name":"EMPLOYEE_TO_REPOSITORY","owner":"DB2INST1","seq":2,"type":"QUERY   "},{"name":"ORG_TO_QMF","owner":"DB2INST1","seq":1,"type":"QUERY   "},{"name":"ORG_TO_QMF","owner":"DB2INST1","seq":2,"type":"QUERY   "}],"objectDirectories":[{"created":"2025-01-09 10:46:01.929","lastUser":"2025-01-22 21:47:55.478313","model":"        ","modified":"2025-01-09 10:46:02.285706","name":"EMPLOYEE_TO_REPOSITORY","objectLevel":2,"owner":"DB2INST1","restricted":"N","subType":"SQL     ","type":"QUERY   "},{"created":"2025-01-09 10:46:47.028","lastUser":"2025-01-09 10:46:47.543515","model":"        ","modified":"2025-01-09 10:46:47.390611","name":"ORG_TO_QMF","objectLevel":2,"owner":"DB2INST1","restricted":"N","subType":"SQL     ","type":"QUERY   "}],"objectRemarks":[{"name":"EMPLOYEE_TO_REPOSITORY","owner":"DB2INST1","remarks":"","type":"QUERY   "},{"name":"ORG_TO_QMF","owner":"DB2INST1","remarks":"","type":"QUERY   "}]}}"""
      val parsed = string.fromJson[IncomingMessage]
      assert(parsed)(Assertion.equalTo(Right("")))
    } @@ TestAspect.ignore
     */
  )
