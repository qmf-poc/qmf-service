package qmf.poc.service.catalog

case class CatalogSnapshot(objectData: Seq[ObjectData],
                           objectRemarks: Seq[ObjectRemarks],
                           objectDirectories: Seq[ObjectDirectory])