package qmf.poc.service.http.handlers.rest

import zio.http.Response

val ping: Response = Response.text("pong")
