package qmf.poc.service.http.handlers.params

import qmf.poc.service.agent.AgentId
import zio.{IO, ZIO}
import zio.http.{Request, URL}

class NoParameterException(parameter: String, request: Request):
  val message = s"Missing parameter: $parameter in URL: ${request.url}"

def param(request: Request, name: String): IO[NoParameterException, String] =
  request.url.queryParams(name).headOption match
    case Some(param) => ZIO.succeed(param)
    case None        => ZIO.fail(NoParameterException(name, request))

def paramAgentId(request: Request): IO[NoParameterException, AgentId] =
  param(request, "agent").map(AgentId(_))
