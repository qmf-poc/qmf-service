package qmf.poc.service.catalog

case class ObjectData(owner: String, name: String, `type`: String, seq: Short, appldata: Array[Byte])
