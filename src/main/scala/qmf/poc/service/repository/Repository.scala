package qmf.poc.service.repository

import qmf.poc.service.catalog.CatalogSnapshot
import zio.IO

trait Repository:
  def load(snapshot: CatalogSnapshot): IO[RepositoryError, Int]
  def persist(qmfObject: QMFObject): IO[RepositoryError, Unit]
  def retrieve(queryString: String): IO[RepositoryError, Seq[QMFObject]]
