package qmf.poc.service.http.handlers.rest

import qmf.poc.service.repository.{QMFObject, Repository}
import zio.ZIO
import zio.http.{Request, Response, Status}
import zio.json.EncoderOps

def query(repository: Repository): Request => ZIO[Any, Nothing, Response] = (request: Request) =>
  val searchParam = request.url.queryParams("search").headOption.getOrElse("*")
  repository
    .query(searchParam)
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
