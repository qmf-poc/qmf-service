package qmf.poc.service.catalog

import zio.json.{DeriveJsonDecoder, JsonDecoder}

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
    lastUser: String
)

object ObjectDirectory:
  given JsonDecoder[ObjectDirectory] = DeriveJsonDecoder.gen[ObjectDirectory]
