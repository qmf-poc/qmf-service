package qmf.poc.service.http.handlers.rest

import qmf.poc.service.repository.{QMFObject, Repository}
import zio.ZIO
import zio.http.{Request, Response, Status}
import zio.json.EncoderOps

def query: Request => ZIO[Repository, Nothing, Response] = (request: Request) =>
  val searchParam = request.url.queryParams("search").headOption.getOrElse("*")
  ZIO.serviceWithZIO[Repository] { repository =>
    repository
      .query(searchParam)
      .flatMap { result =>
        ZIO.succeed(Response.json(result.toJson))
      }
      .catchAll { error =>
        ZIO.logError(error.message).as {
          Response
            .text(error.message)
            .status(Status.BadRequest)
        }
      }
  }
