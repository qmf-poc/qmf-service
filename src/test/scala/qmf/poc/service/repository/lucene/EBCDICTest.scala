package qmf.poc.service.repository.lucene

class EBCDICTest extends munit.FunSuite:
  test("String should decode IBM037 as EBCDIC"):
    val aAi9 = Array[Byte](0x81.toByte, 0xc1.toByte, 0x89.toByte, 0xf9.toByte);
    val s = String(aAi9, "IBM037")
    assertEquals(s, "aAi9")

  test("String should decode IBM1047 as EBCDIC"):
    val aAi9 = Array[Byte](0x81.toByte, 0xc1.toByte, 0x89.toByte, 0xf9.toByte);
    val s = String(aAi9, "IBM1047")
    assertEquals(s, "aAi9")
