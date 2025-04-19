package qmf.poc.service.qmfstorage.lucene

import java.nio.charset.Charset

class EBCDICTest extends munit.FunSuite:
  test("String should decode IBM037 as EBCDIC"):
    val aAi9 = Array[Byte](0x81.toByte, 0xc1.toByte, 0x89.toByte, 0xf9.toByte);
    val s = String(aAi9, "IBM037")
    assertEquals(s, "aAi9")

  test("String should decode IBM1047 as EBCDIC"):
    val aAi9 = Array[Byte](0x81.toByte, 0xc1.toByte, 0x89.toByte, 0xf9.toByte);
    val s = String(aAi9, "IBM1047")
    assertEquals(s, "aAi9")

  test("String should be encoded to IBM1047 from UTF-8"):
    def toEBCDIC(s: String): Array[Byte] = s.getBytes(Charset.forName("IBM1047"))
    def toUTF8(b: Array[Byte]): String = new String(b, "IBM1047")
    val aAi9 = toEBCDIC("aAi9")
    assertEquals(toUTF8(aAi9), "aAi9")

  test("String should be encoded to 1037 from UTF-8"):
    def toEBCDIC(s: String): Array[Byte] = s.getBytes(Charset.forName("IBM037"))
    // def toUTF8(b: Array[Byte]): String = new String(b, "IBM1047")
    val aAi9expected = Array[Byte](0x81.toByte, 0xc1.toByte, 0x89.toByte, 0xf9.toByte);
    val aAi9 = toEBCDIC("aAi9")
    assertEquals(aAi9.length, 4)
    assertEquals(aAi9expected.length, 4)
    assertEquals(aAi9(0), aAi9expected(0))
    assertEquals(aAi9(1), aAi9expected(1))
    assertEquals(aAi9(2), aAi9expected(2))
    assertEquals(aAi9(3), aAi9expected(3))
