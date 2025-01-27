package qmf.poc.service.http.handlers.rest

import qmf.poc.service.repository.{QMFObject, Repository}
import zio.ZIO
import zio.durationInt
import zio.http.{Request, Response, Status}
import zio.json.{DeriveJsonEncoder, JsonEncoder, given}

given JsonEncoder[QMFObject] = DeriveJsonEncoder.gen[QMFObject]

def retrieve(repository: Repository): Request => ZIO[Any, Nothing, Response] = (request: Request) =>
  val searchParam = request.url.queryParams("search").headOption.getOrElse("*")
  repository.retrieve(searchParam).map { result =>
      Response.json(result.toJson) // Successful response with serialized JSON
    }
    .catchAll { error =>
      // Handle error and return 400 Bad Request with error message
      ZIO.logError(error.getMessage).as {
        Response
          .text(error.getMessage)
          .status(Status.BadRequest)
      }
    }