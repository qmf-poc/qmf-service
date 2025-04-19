package qmf.poc.service.http.handlers.queries

import qmf.poc.service.http.handlers.params.{NoParameterException, param, paramAgentId}
import qmf.poc.service.qmfstorage.{QMFObject, QmfObjectsStorage, QmfObjectsStorageError}
import zio.{Cause, ZIO}
import zio.http.{Request, Response, Status}
import zio.json.EncoderOps

def query: Request => ZIO[QmfObjectsStorage, Nothing, Response] = (request: Request) =>
  (for {
    agentId <- paramAgentId(request)
    search <- param(request, "search")
    result <- ZIO.serviceWithZIO[QmfObjectsStorage] { _.query(agentId, search) }
  } yield Response.json(result.toJson))
    .catchAll {
      case e: NoParameterException =>
        ZIO
          .logErrorCause(e.message, Cause.fail(e))
          .as(
            Response.text(e.message).status(Status.BadRequest)
          )
      case e: QmfObjectsStorageError =>
        ZIO
          .logErrorCause(e.message, Cause.fail(e))
          .as(
            Response.text(e.message).status(Status.InternalServerError)
          )
    }
