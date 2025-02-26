package qmf.poc.service.repository

import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.document.*
import org.apache.lucene.index.{DirectoryReader, IndexNotFoundException, IndexWriter, IndexWriterConfig}
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.search.{IndexSearcher, MatchAllDocsQuery}
import org.apache.lucene.store.{ByteBuffersDirectory, Directory}
import qmf.poc.service.catalog.{CatalogSnapshot, ObjectData, ObjectDirectory, ObjectRemarks}
import qmf.poc.service.repository.*
import zio.{CanFail, FiberRef, IO, Task, ULayer, ZIO, ZLayer}

type LuceneId = Int

class RepositoryError(th: Throwable) extends Exception(th.getMessage, th)

private class CatalogSnapshotItem(val data: ObjectData, val directory: ObjectDirectory, val remarks: ObjectRemarks)

class LuceneRepository(directory: Directory) extends Repository:
  private val analyzer = new StandardAnalyzer
  private val index = directory // new ByteBuffersDirectory
  private val config = IndexWriterConfig(analyzer)
  private val w = new IndexWriter(index, config)

  // TODO: refactor
  private var rOpt: Option[DirectoryReader] = None

  private def r: DirectoryReader =
    rOpt match
      case Some(reader) =>
        val updatedReader = DirectoryReader.openIfChanged(reader, w)
        if (updatedReader == null) reader
        else
          rOpt = Some(updatedReader)
          updatedReader
      case None =>
        val newReader = DirectoryReader.open(index)
        rOpt = Some(newReader)
        newReader

  // TODO: blocking
  override def load(snapshot: CatalogSnapshot): IO[RepositoryError, Int] = ZIO.scoped(for {
    _ <- ZIO
      .attemptBlocking {
        w.deleteAll()
        w.commit()
        // initializeIndex()
      }
      .onError(cause => ZIO.logErrorCause(cause))
      .mapError(th => RepositoryError(th))
    data = snapshot.objectData.map { d => (s"${d.owner}?${d.name}?${d.`type`}", d) }.toMap
    remarks = snapshot.objectRemarks.map { d => (s"${d.owner}?${d.name}?${d.`type`}", d) }.toMap
    directories = snapshot.objectDirectories.map { d => (s"${d.owner}?${d.name}?${d.`type`}", d) }.toMap
    keys = data.keySet.intersect(remarks.keySet).intersect(directories.keySet)
    counter <- FiberRef.make(0)
    _ <- ZIO.foreachDiscard(keys) { key =>
      val od = data.get(key)
      val or = remarks.get(key)
      val odi = directories.get(key)
      (od, or, odi) match {
        case (Some(odValue), Some(orValue), Some(odiValue)) =>
          val parts = key.split('?')
          persist(QMFObject(odValue.owner, orValue.name, orValue.`type`, odValue.appldata))
            .tap(Unit => counter.update(_ + 1))
            .onError(cause => ZIO.logErrorCause(cause))
        case _ =>
          ZIO.logWarning(s"Missing data for key: $key (od: $od, or: $or, odi: $odi)")
      }
    }
    c <- counter.get
  } yield c)

  override def persist(qmfObject: QMFObject): IO[RepositoryError, Unit] = for
    exists <- has(luceneId(qmfObject))
    _ <- ZIO.when(!exists)(add(qmfObject))
    _ <- ZIO.logDebug(s"persist: $qmfObject")
  yield ()

  // TODO: currently the most simple search: "any substring of all fields"
  //      improvements:
  //      - detect * and ? in the query string and use WildcardQuery
  //      - detect : in the query string and limit the search to the field before the :
  def retrieve(queryString: String): IO[RepositoryError, Seq[QMFObject]] = ZIO
    .attemptBlocking {
      // TODO: IndexSearcher should be memoized
      val s = new IndexSearcher(r)
      val queryParser = new QueryParser("record", analyzer)
      queryParser.setAllowLeadingWildcard(true)
      val query =
        if (queryString.length <= 2)
          new MatchAllDocsQuery()
        else {
          queryParser.parse(s"*$queryString*")
        }
      val results = s.search(query, Int.MaxValue)
      val storedFields = s.storedFields()
      val hits = results.scoreDocs
      hits
        .map(hit => {
          val doc = storedFields.document(hit.doc)
          val owner = doc.get("owner")
          val name = doc.get("name")
          val typ = doc.get("type")
          val appldata = doc.get("appldata")
          QMFObject(owner, name, typ, appldata)
        })
        .toSeq
    }
    .catchSome { case _: IndexNotFoundException =>
      ZIO.succeed(Seq())
    }
    .mapError(th => RepositoryError(th))

  private inline def luceneId(a: Any): LuceneId =
    a.hashCode()

  private def has(id: LuceneId): IO[RepositoryError, Boolean] = ZIO
    .attempt {
      val query = IntPoint.newExactQuery("ObjectId", id)
      val s = new IndexSearcher(r)
      val results = s.search(query, 1)
      results.totalHits.value() > 0
    }
    .catchSome { case _: IndexNotFoundException =>
      ZIO.succeed(false)
    }
    .mapError(th => RepositoryError(th))

  private def add(qmfObject: QMFObject): IO[RepositoryError, Unit] = ZIO
    .attempt {
      val doc = new Document()
      doc.add(new IntPoint("id", luceneId(qmfObject)))
      val record = qmfObject.name + " " + qmfObject.typ + " " + qmfObject.owner
      doc.add(new TextField("record", record, Field.Store.YES))
      doc.add(new StoredField("owner", qmfObject.owner))
      doc.add(new StoredField("name", qmfObject.name))
      doc.add(new StoredField("type", qmfObject.typ))
      doc.add(new TextField("appldata", qmfObject.applData, Field.Store.YES))
      w.addDocument(doc)
      w.commit()
      ()
    }
    .mapError(th => RepositoryError(th))

object LuceneRepository:
  def apply(directory: Directory = new ByteBuffersDirectory()) = new LuceneRepository(directory: Directory)

  val layer: ULayer[Repository] = ZLayer.succeed(LuceneRepository())
