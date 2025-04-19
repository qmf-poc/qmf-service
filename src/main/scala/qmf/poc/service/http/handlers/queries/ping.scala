package qmf.poc.service.http.handlers.queries

import zio.http.Response

val ping: Response = Response.text("pong")
