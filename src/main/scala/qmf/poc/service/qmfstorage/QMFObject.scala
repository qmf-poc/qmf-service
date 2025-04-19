package qmf.poc.service.qmfstorage

import zio.json.ast.Json
import zio.json.{DeriveJsonEncoder, JsonEncoder, given}

case class QMFObject(owner: String, name: String, typ: String, remarks: String, applData: String):
  val id: String = s"$owner-$name-$typ".trim

  override def toString: String = id

object QMFObject:
  def apply(owner: String, name: String, typ: String, remarks: String, applData: String) =
    new QMFObject(owner, name, typ, remarks, applData)

  // compatibility with the previous version
  def apply(owner: String, name: String, typ: String, applData: String) =
    new QMFObject(owner, name, typ, "", applData)

  // given JsonEncoder[QMFObject] = DeriveJsonEncoder.gen[QMFObject]: contrmap adds ID
  given JsonEncoder[QMFObject] = JsonEncoder[Map[String, Json]].contramap { obj =>
    Map(
      "owner" -> Json.Str(obj.owner),
      "name" -> Json.Str(obj.name),
      "typ" -> Json.Str(obj.typ),
      "remarks" -> Json.Str(obj.remarks),
      "applData" -> Json.Str(obj.applData),
      "id" -> Json.Str(obj.id)
    )
  }
