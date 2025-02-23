package qmf.poc.service.catalog

case class CatalogSnapshot(objectData: Seq[ObjectData], objectRemarks: Seq[ObjectRemarks], objectDirectories: Seq[ObjectDirectory]):
  override def toString: String = {
    s"CatalogSnapshot(objectData=${objectData.length}, objectRemarks=${objectRemarks.length}, objectDirectories=${objectDirectories.length})"
  }
