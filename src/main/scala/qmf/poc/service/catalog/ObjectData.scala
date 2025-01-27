package qmf.poc.service.catalog

case class ObjectData(val owner: String, val name: String, `type`: String, val seq: Short, val appldata: Array[Byte])
