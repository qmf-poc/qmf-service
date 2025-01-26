package qmf.poc.service.catalog

import scala.collection.mutable.ArrayBuffer

class Catalog(val objectData: ArrayBuffer[ObjectData],
              val objectRemarks: ArrayBuffer[ObjectRemarks],
              val objectDirectory: ArrayBuffer[ObjectDirectory])
