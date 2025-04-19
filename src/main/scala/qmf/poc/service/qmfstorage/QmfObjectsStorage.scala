package qmf.poc.service.qmfstorage

import qmf.poc.service.agent.AgentId
import qmf.poc.service.catalog.CatalogSnapshot
import zio.IO

trait QmfObjectsStorage:
  def load(agentId: AgentId, snapshot: CatalogSnapshot): IO[QmfObjectsStorageError, Int]
  def persist(i: Int, qmfObject: QMFObject): IO[QmfObjectsStorageError, Unit]
  def query(agentId: AgentId, queryString: String): IO[QmfObjectsStorageError, Seq[QMFObject]]
  def get(agentId: AgentId, id: String): IO[QmfObjectsStorageError, QMFObject]
