package qmf.poc.service.catalog

import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}

case class ObjectAgent(
    owner: String,
    name: String,
    `type`: String,
    subType: String,
    objectLevel: Int,
    restricted: String,
    model: String,
    created: String,
    modified: String,
    lastUsed: String,
    appldata: String,
    remarks: String
)

object ObjectAgent:
  given JsonDecoder[ObjectAgent] = DeriveJsonDecoder.gen[ObjectAgent]
  given JsonEncoder[ObjectAgent] = DeriveJsonEncoder.gen[ObjectAgent]
