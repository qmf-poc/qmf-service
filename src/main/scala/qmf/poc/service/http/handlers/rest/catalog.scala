package qmf.poc.service.http.handlers.rest

import qmf.poc.service.http.handlers.ws.Broker
import zio.http.Response

def catalog(broker: Broker) = () =>  Response.text("Hello, world!")
