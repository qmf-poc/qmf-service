package qmf.poc.service.repository

import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.document.*
import org.apache.lucene.index.{DirectoryReader, IndexWriter, IndexWriterConfig}
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.store.{ByteBuffersDirectory, Directory}
import qmf.poc.service.catalog.{CatalogSnapshot, ObjectData}
import qmf.poc.service.repository.*
import zio.{CanFail, IO, ULayer, ZIO, ZLayer}

type LuceneId = Int

class RepositoryError(val th: Throwable) extends Exception

class LuceneRepository(directory: Directory) extends Repository:
  private val analyzer = new StandardAnalyzer
  private val index = directory //new ByteBuffersDirectory
  private val config = IndexWriterConfig(analyzer)
  private val w = new IndexWriter(index, config)

  private def initializeIndex(): Unit = {
    if (DirectoryReader.indexExists(index)) {
    } else {
      val doc = new Document()
      doc.add(new IntPoint("id", 0))
      w.addDocument(doc)
      w.commit()
    }
  }

  initializeIndex()

  private val r = DirectoryReader.open(index)
  private val s = new IndexSearcher(r)

  // TODO: blocking
  override def load(snapshot: CatalogSnapshot): IO[RepositoryError, Unit] =
    w.deleteAll()
    initializeIndex()
    val objectData = snapshot.objectData.map { d => (s"${d.owner}?{${d.name}?${d.`type`}", d) }.toMap
    val objectRemarks = snapshot.objectRemarks.map { d => (s"${d.owner}?{${d.name}?${d.`type`}", d) }.toMap
    val objectDirectories = snapshot.objectDirectories.map { d => (s"${d.owner}?{${d.name}?${d.`type`}", d) }.toMap
    val keys = objectData.keySet.intersect(objectRemarks.keySet).intersect(objectDirectories.keySet)

    val effects = keys.toList.map { key =>
      val od = objectData.get(key)
      val or = objectRemarks.get(key)
      val odi = objectDirectories.get(key)

      (od, or, odi) match {
        case (Some(odValue), Some(orValue), Some(odiValue)) =>
          val parts = key.split('?')
          persist(QMFObject(odValue.owner, orValue.name, orValue.`type`))
        case _ =>
          ZIO.logWarning(s"Missing data for key: $key (od: $od, or: $or, odi: $odi)")
      }
    }
    ZIO.collectAllDiscard(effects)

  override def persist(qmfObject: QMFObject): IO[RepositoryError, Unit] = for
    exists <- has(luceneId(qmfObject))
    _ <- ZIO.when(exists)(add(qmfObject))
    _ <- ZIO.logDebug(s"persist: $qmfObject")
  yield ()


  /*for
      exists <- has(luceneId(qmfObject))
      _ <- ZIO.unless(exists)(add(qmfObject))
    yield ()*/

  // TODO: currently the most simple search: "any substring of all fields"
  //      improvements:
  //      - detect * and ? in the query string and use WildcardQuery
  //      - detect : in the query string and limit the search to the field before the :
  def retrieve(queryString: String): IO[RepositoryError, Seq[QMFObject]] = ZIO.attemptBlocking {
    val queryParser = new QueryParser("record", analyzer)
    val query = queryParser.parse(queryString)
    val results = s.search(query, 10)
    val storedFields = s.storedFields()
    val hits = results.scoreDocs
    hits.map(hit => {
      val doc = storedFields.document(hit.doc)
      QMFObject(doc.get("owner"), doc.get("name"), doc.get("type"))
    }).toSeq
  }.mapError(th => RepositoryError(th))

  private inline def luceneId(a: Any): LuceneId =
    a.hashCode()

  private def has(id: LuceneId): IO[RepositoryError, Boolean] = ZIO.attempt {
    val query = IntPoint.newExactQuery("ObjectId", id)
    val results = s.search(query, 1)
    results.totalHits.value() > 0
  }.mapError(th => RepositoryError(th))

  private def add(qmfObject: QMFObject): IO[RepositoryError, Unit] = ZIO.attempt {
    val doc = new Document()
    doc.add(new IntPoint("id", luceneId(qmfObject)))
    doc.add(new TextField("record", qmfObject.owner + " " + qmfObject.name + " " + qmfObject.typ, Field.Store.YES))
    doc.add(new StoredField("owner", qmfObject.owner))
    doc.add(new StoredField("name", qmfObject.name))
    doc.add(new StoredField("type", qmfObject.typ))
    w.addDocument(doc)
    w.commit()
    ()
  }.mapError(th => RepositoryError(th))

object LuceneRepository:
  def apply(directory: Directory = new ByteBuffersDirectory()) = new LuceneRepository(directory: Directory)

  val layer: ULayer[Repository] = ZLayer.succeed(LuceneRepository())