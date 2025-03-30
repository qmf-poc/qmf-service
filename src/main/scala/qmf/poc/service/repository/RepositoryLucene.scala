package qmf.poc.service.repository

import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.document.*
import org.apache.lucene.index.*
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.search.{IndexSearcher, MatchAllDocsQuery, TopDocs}
import org.apache.lucene.store.{ByteBuffersDirectory, Directory}
import qmf.poc.service.catalog.{CatalogSnapshot, ObjectData, ObjectDirectory, ObjectRemarks}
import qmf.poc.service.repository.*
import qmf.poc.service.repository.LuceneRepository.topDocs2Objects
import zio.{CanFail, FiberRef, IO, ULayer, ZIO, ZLayer}

type LuceneId = Int

sealed trait RepositoryError:
  def message: String

class RepositoryErrorObjectNotFound(id: String) extends RepositoryError:
  def message: String = s"Object id=$id not found"

class RepositoryErrorThrowable(th: Throwable) extends Exception(th.getMessage, th) with RepositoryError:
  def message: String = th.getMessage

given Conversion[RepositoryError, Throwable] with
  def apply(error: RepositoryError): Throwable = error match
    case e: Throwable                     => e
    case e: RepositoryErrorObjectNotFound => Exception(e.message)

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
      .mapError(th => RepositoryErrorThrowable(th))
    data = snapshot.objectData.map { d => (s"${d.owner}?${d.name}?${d.`type`}", d) }.toMap
    remarks = snapshot.objectRemarks.map { d => (s"${d.owner}?${d.name}?${d.`type`}", d) }.toMap
    directories = snapshot.objectDirectories.map { d => (s"${d.owner}?${d.name}?${d.`type`}", d) }.toMap
    keys = data.keySet.intersect(remarks.keySet).intersect(directories.keySet)
    counter <- FiberRef.make(0)
    _ <- ZIO.foreachDiscard(keys) { key =>
      val od = data.get(key)
      val or = remarks.get(key).orElse(Some(ObjectRemarks("", "", "", "")))
      val odi = directories.get(key)
      (od, or, odi) match {
        case (Some(odValue), Some(orValue), Some(odiValue)) =>
          val parts = key.split('?')
          persist(
            QMFObject(odValue.owner.trim, odValue.name.trim, odValue.`type`.trim, orValue.remarks.trim, odValue.appldata.trim)
          )
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

  def query(queryString: String): IO[RepositoryError, Seq[QMFObject]] = ZIO
    .attemptBlocking {
      // TODO: IndexSearcher should be memoized
      val s = new IndexSearcher(r)
      val queryParser = new QueryParser("record", analyzer)
      queryParser.setAllowLeadingWildcard(true)
      val query =
        if (queryString.length <= 2)
          new MatchAllDocsQuery()
        else if (queryString.contains(':')) {
          queryParser.parse(queryString)
        } else {
          queryParser.parse(s"*$queryString*")
        }
      val results = s.search(query, Int.MaxValue)
      val storedFields = s.storedFields()
      topDocs2Objects(results, storedFields).toSeq
    }
    .catchSome { case _: IndexNotFoundException =>
      ZIO.succeed(Seq())
    }
    .mapError(th => RepositoryErrorThrowable(th))

  private inline def luceneId(s: String): LuceneId =
    s.hashCode

  private inline def luceneId(o: QMFObject): LuceneId =
    luceneId(o.id)

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
    .mapError(th => RepositoryErrorThrowable(th))

  def get(id: String): IO[RepositoryError, QMFObject] =
    ZIO
      .attempt {
        val query = IntPoint.newExactQuery("ObjectId", luceneId(id))
        val s = new IndexSearcher(r)
        val results = s.search(query, 1)
        topDocs2Objects(results, s.storedFields())
      }
      .catchSome { case _: IndexNotFoundException =>
        ZIO.succeed(Array[QMFObject]())
      }
      .mapError(th => RepositoryErrorThrowable(th))
      .flatMap { objects =>
        objects.headOption match {
          case Some(firstObject) => ZIO.succeed(firstObject)
          case None              => ZIO.fail(RepositoryErrorObjectNotFound(id))
        }
      }

  private def add(qmfObject: QMFObject): IO[RepositoryError, Unit] = ZIO
    .attempt {
      val doc = new Document()
      doc.add(new IntPoint("ObjectId", luceneId(qmfObject)))
      val record = qmfObject.name + " " + qmfObject.typ + " " + qmfObject.owner
      doc.add(new TextField("record", record, Field.Store.YES))
      doc.add(new StoredField("owner", qmfObject.owner))
      doc.add(new StoredField("name", qmfObject.name))
      doc.add(new StoredField("type", qmfObject.typ))
      doc.add(new TextField("appldata", qmfObject.applData, Field.Store.YES))
      // if (qmfObject.remarks.nonEmpty) - hm, i expect the empty fiels is not necessery
      // but it causes some error
      doc.add(new TextField("remarks", qmfObject.remarks, Field.Store.YES))
      w.addDocument(doc)
      w.commit()
      ()
    }
    .mapError(th => RepositoryErrorThrowable(th))

object LuceneRepository:
  def apply(directory: Directory = new ByteBuffersDirectory()) = new LuceneRepository(directory: Directory)

  val layer: ULayer[Repository] = ZLayer.succeed(LuceneRepository())

  private def topDocs2Objects(topDocs: TopDocs, storedFields: StoredFields): Array[QMFObject] =
    val hits = topDocs.scoreDocs
    hits
      .map(hit => {
        val doc = storedFields.document(hit.doc)
        val owner = doc.get("owner")
        val name = doc.get("name")
        val typ = doc.get("type")
        val remarks = doc.get("remarks")
        val appldata = doc.get("appldata")
        QMFObject(owner, name, typ, remarks, appldata)
      })
