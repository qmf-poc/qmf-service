package qmf.poc.service.catalog

import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}

case class ObjectDirectory(
    owner: String,
    name: String,
    `type`: String,
    subType: String,
    objectLevel: Int,
    restricted: String,
    model: String,
    created: String,
    modified: String,
    lastUsed: String
)

object ObjectDirectory:
  given JsonDecoder[ObjectDirectory] = DeriveJsonDecoder.gen[ObjectDirectory]
  given JsonEncoder[ObjectDirectory] = DeriveJsonEncoder.gen[ObjectDirectory]
