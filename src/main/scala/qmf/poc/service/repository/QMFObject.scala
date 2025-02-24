package qmf.poc.service.repository

case class QMFObject(owner: String, name: String, typ: String, applData: String)

object QMFObject:
  def apply(owner: String, name: String, typ: String, applData: Array[Byte]) =
    new QMFObject(owner, name, typ, applData.toUTF8)

  extension (a: Array[Byte]) def toUTF8: String = new String(a, "IBM1047")
