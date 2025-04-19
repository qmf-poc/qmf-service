package qmf.poc.service.messages

import qmf.poc.service.agent.*
import qmf.poc.service.catalog.CatalogSnapshot
import zio.Random.RandomLive
import zio.json.ast.Json
import zio.json.{DeriveJsonEncoder, JsonCodecConfiguration, JsonDecoder, JsonEncoder}
import zio.{Random, UIO, ULayer, ZEnvironment, ZIO, ZLayer}

trait OutgoingMessageIdGenerator:
  def nextId: UIO[Int]

object OutgoingMessageIdGenerator:
  val random: ZIO[Random, Nothing, OutgoingMessageIdGenerator] = ZIO.serviceWith[Random] { random =>
    new OutgoingMessageIdGenerator {
      def nextId: UIO[Int] = random.nextInt
    }
  }
  val layer: ULayer[OutgoingMessageIdGenerator] = ZLayer.succeed(RandomLive) >>> ZLayer(random)
