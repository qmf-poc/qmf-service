package qmf.poc.service.catalog

import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}

case class CatalogSnapshot(qmfObjects: Seq[ObjectAgent]):
  override def toString: String = {
    s"CatalogSnapshot(${qmfObjects.length})"
  }
  
object CatalogSnapshot:
  given JsonDecoder[CatalogSnapshot] = DeriveJsonDecoder.gen[CatalogSnapshot]
  given JsonEncoder[CatalogSnapshot] = DeriveJsonEncoder.gen[CatalogSnapshot]
