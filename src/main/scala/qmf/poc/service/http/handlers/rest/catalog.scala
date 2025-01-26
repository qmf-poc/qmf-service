package qmf.poc.service.http.handlers.rest

import qmf.poc.service.http.handlers.ws.{Broker, RequestSnapshot}
import zio.{IO, ZIO}
import zio.http.{Handler, Response}

def catalog(broker: Broker) = () =>  Response.text("Hello, world!")
