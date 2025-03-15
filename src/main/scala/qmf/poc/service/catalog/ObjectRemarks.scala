package qmf.poc.service.catalog

import zio.json.{DeriveJsonDecoder, JsonDecoder}

case class ObjectRemarks(
  owner: String,
  name: String,
  `type`: String,
  remarks: String,
)

object ObjectRemarks:
  given JsonDecoder[ObjectRemarks] = DeriveJsonDecoder.gen[ObjectRemarks]
