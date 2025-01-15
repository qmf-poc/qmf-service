package qmf.poc.service.http

import qmf.poc.service.http.rest.ping
import zio.http.Routes

package object http
val routes = Routes(ping)
