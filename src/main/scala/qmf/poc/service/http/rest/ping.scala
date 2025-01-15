package qmf.poc.service.http.rest

import zio.http.*
import zio.http.Method.GET

val ping = GET / "ping" -> handler(Response.text("pong"))
