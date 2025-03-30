package qmf.poc.service.catalog

import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}

case class CatalogSnapshot(qmfObjects: Seq[ObjectAgent]):
  override def toString: String = {
    s"CatalogSnapshot(${qmfObjects.length})"
  }
  /*
case class CatalogSnapshot(objectData: Seq[ObjectData], objectRemarks: Seq[ObjectRemarks], objectDirectories: Seq[ObjectDirectory]):
  override def toString: String = {
    s"CatalogSnapshot(objectData=${objectData.length}, objectRemarks=${objectRemarks.length}, objectDirectories=${objectDirectories.length})"
  }*/

object CatalogSnapshot:
  given JsonDecoder[CatalogSnapshot] = DeriveJsonDecoder.gen[CatalogSnapshot]
  given JsonEncoder[CatalogSnapshot] = DeriveJsonEncoder.gen[CatalogSnapshot]
