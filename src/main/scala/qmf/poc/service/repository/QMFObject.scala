package qmf.poc.service.repository

case class QMFObject(owner: String, name: String, typ: String, remarks: String, applData: String)

object QMFObject:
  def apply(owner: String, name: String, typ: String, remarks: String, applData: Array[Byte]) =
    new QMFObject(owner, name, typ, remarks, applData.toUTF8)

  // compatibility with the previous version
  def apply(owner: String, name: String, typ: String, applData: String) =
    new QMFObject(owner, name, typ, "", applData)

  extension (a: Array[Byte]) def toUTF8: String = new String(a, "IBM1047")
