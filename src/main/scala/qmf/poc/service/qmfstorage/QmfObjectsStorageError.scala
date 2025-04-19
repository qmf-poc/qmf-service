package qmf.poc.service.qmfstorage

sealed trait QmfObjectsStorageError:
  def message: String

class QmfObjectsStorageErrorObjectNotFound(id: String) extends QmfObjectsStorageError:
  def message: String = s"Object id=$id not found"

class QmfObjectsStorageErrorThrowable(th: Throwable) extends Exception(th.getMessage, th) with QmfObjectsStorageError:
  def message: String = th.getMessage

object QmfObjectsStorageError:
  given Conversion[QmfObjectsStorageError, Throwable] with
    def apply(error: QmfObjectsStorageError): Throwable = error match
      case e: Throwable                            => e
      case e: QmfObjectsStorageErrorObjectNotFound => Exception(e.message)
