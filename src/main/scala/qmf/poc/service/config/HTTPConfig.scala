package qmf.poc.service.config

import zio.config.*
import zio.*
import zio.Config.int

case class HTTPConfig(port: Int)

val HTTPConfigDescriptor: Config[HTTPConfig] = (int("PORT") ?? "HTTP server's listening port").to[HTTPConfig]
