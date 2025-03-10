package qmf.poc.service.http.handlers.rest

import qmf.poc.service.repository.{QMFObject, Repository}
import zio.ZIO
import zio.http.{Request, Response, Status}
import zio.json.EncoderOps

def get(repository: Repository): Request => ZIO[Any, Nothing, Response] = (request: Request) =>
  val searchParam = request.url.queryParams("id").headOption.getOrElse("null")
  repository
    .get(searchParam)
    .map { result =>
      Response.json(result.toJson)
    }
    .catchAll { error =>
      ZIO.logError(error.message).as {
        Response
          .text(error.message)
          .status(Status.BadRequest)
      }
    }
