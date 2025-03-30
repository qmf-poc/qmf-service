package qmf.poc.service.repository

import qmf.poc.service.catalog.CatalogSnapshot
import zio.IO

trait Repository:
  def load(snapshot: CatalogSnapshot): IO[RepositoryError, Int]
  def persist(i: Int, qmfObject: QMFObject): IO[RepositoryError, Unit]
  def query(queryString: String): IO[RepositoryError, Seq[QMFObject]]
  def get(id: String): IO[RepositoryError, QMFObject]
