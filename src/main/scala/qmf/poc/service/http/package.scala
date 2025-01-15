package qmf.poc.service.http

import qmf.poc.service.http.handlers.rest.ping
import qmf.poc.service.http.handlers.ws.echo
import zio.http.Method.GET
import zio.http.{Routes, handler}

package object http

val routes = Routes(
  GET / "ping" -> handler(ping),
  GET / "echo" -> handler(echo.toResponse),
)
