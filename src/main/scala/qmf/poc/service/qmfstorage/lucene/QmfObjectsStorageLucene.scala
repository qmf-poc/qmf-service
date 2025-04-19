package qmf.poc.service.qmfstorage.lucene

import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.document.*
import org.apache.lucene.index.*
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.search.{IndexSearcher, MatchAllDocsQuery, TopDocs}
import org.apache.lucene.store.{ByteBuffersDirectory, Directory}
import qmf.poc.service.catalog.{CatalogSnapshot, ObjectAgent}
import QmfObjectsStorageLucene.topDocs2Objects
import qmf.poc.service.agent.AgentId
import qmf.poc.service.qmfstorage.*
import zio.{CanFail, FiberRef, IO, ULayer, ZIO, ZLayer}

class QmfObjectsStorageLucene(directory: Directory) extends QmfObjectsStorage:
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
  override def load(snapshot: CatalogSnapshot): IO[QmfObjectsStorageError, Int] = ZIO.scoped(for {
    _ <- ZIO
      .attemptBlocking {
        w.deleteAll()
        w.commit()
      }
      .onError(cause => ZIO.logErrorCause(cause))
      .mapError(th => QmfObjectsStorageErrorThrowable(th))
    counter <- FiberRef.make(0)
    _ <- ZIO.foreachDiscard(snapshot.qmfObjects) { obj =>
      counter
        .updateAndGet(_ + 1)
        .flatMap((n: Int) =>
          persist(
            n,
            QMFObject(obj.owner.trim, obj.name.trim, obj.`type`.trim, obj.remarks.trim, obj.appldata.trim)
          )
        )
    }
    c <- counter.get
  } yield c)

  override def persist(agentId: AgentId, logNo: Int, qmfObject: QMFObject): IO[QmfObjectsStorageError, Unit] = for
    exists <- has(luceneId(qmfObject))
    _ <- ZIO.when(!exists)(add(agentId, qmfObject))
    _ <- ZIO.logDebug(s"persist($logNo): $qmfObject")
  yield ()

  def query(agentId: AgentId, queryString: String): IO[QmfObjectsStorageError, Seq[QMFObject]] = (for {
    s <- ZIO.attemptBlocking { new IndexSearcher(r) }
    query <- ZIO.attemptBlocking {
      val s = new IndexSearcher(r)
      val queryParser = new QueryParser("record", analyzer)
      queryParser.setAllowLeadingWildcard(true)
      if (queryString.length <= 2)
        new MatchAllDocsQuery()
      else if (queryString.contains(':')) {
        queryParser.parse(queryString)
      } else {
        queryParser.parse(s"*$queryString*")
      }
    }
    _ <- ZIO.logDebug(s"Query: $query")
    result <- ZIO.attemptBlocking(s.search(query, Int.MaxValue))
    _ <- ZIO.logDebug(s"Results hits: ${result.totalHits}")
    docs <- ZIO.attemptBlocking(topDocs2Objects(result, s.storedFields(), agentId).toSeq)
    _ <- ZIO.logDebug(s"Final docs count: ${docs.length}")
  } yield docs)
    .catchSome { case _: IndexNotFoundException =>
      ZIO.succeed(Seq())
    }
    .tapError(th => ZIO.logError(th.getMessage))
    .mapError(th => QmfObjectsStorageErrorThrowable(th))
    .tap(seq => ZIO.logDebug(s"query returns ${seq.size} documents"))

  private inline def luceneId(o: QMFObject): LuceneId =
    LuceneId(o.id)

  private def has(id: LuceneId): IO[QmfObjectsStorageError, Boolean] = ZIO
    .attempt {
      val query = IntPoint.newExactQuery("ObjectId", id)
      val s = new IndexSearcher(r)
      val results = s.search(query, 1)
      results.totalHits.value() > 0
    }
    .catchSome { case _: IndexNotFoundException =>
      ZIO.succeed(false)
    }
    .mapError(th => QmfObjectsStorageErrorThrowable(th))

  def get(agentId: AgentId, id: String): IO[QmfObjectsStorageError, QMFObject] =
    ZIO
      .attempt {
        val query = IntPoint.newExactQuery("ObjectId", LuceneId(id).value)
        val s = new IndexSearcher(r)
        val results = s.search(query, 1)
        topDocs2Objects(results, s.storedFields(), agentId)
      }
      .catchSome { case _: IndexNotFoundException =>
        ZIO.succeed(Array[QMFObject]())
      }
      .mapError(th => QmfObjectsStorageErrorThrowable(th))
      .flatMap { objects =>
        objects.headOption match {
          case Some(firstObject) => ZIO.succeed(firstObject)
          case None              => ZIO.fail(QmfObjectsStorageErrorObjectNotFound(id))
        }
      }

  private def add(agentId: AgentId, qmfObject: QMFObject): IO[QmfObjectsStorageError, Unit] = ZIO
    .attemptBlocking {
      val doc = new Document()
      doc.add(new IntPoint("ObjectId", luceneId(qmfObject)))
      val record = qmfObject.name + " " + qmfObject.typ + " " + qmfObject.owner
      doc.add(new TextField("record", record, Field.Store.YES))
      doc.add(new StoredField("agenId", agentId.value))
      doc.add(new StoredField("owner", qmfObject.owner))
      doc.add(new StoredField("name", qmfObject.name))
      doc.add(new StoredField("type", qmfObject.typ))
      doc.add(new TextField("appldata", qmfObject.applData, Field.Store.YES))
      doc.add(new TextField("remarks", qmfObject.remarks, Field.Store.YES))
      w.addDocument(doc)
      w.commit()
      ()
    }
    .mapError(th => QmfObjectsStorageErrorThrowable(th))

object QmfObjectsStorageLucene:
  def apply(directory: Directory = new ByteBuffersDirectory()) = new QmfObjectsStorageLucene(directory: Directory)

  val layer: ULayer[QmfObjectsStorage] = ZLayer.succeed(QmfObjectsStorageLucene())

  // TODO: performance
  private def topDocs2Objects(topDocs: TopDocs, storedFields: StoredFields, ofAgent: AgentId): Array[QMFObject] =
    val hits = topDocs.scoreDocs
    hits
      .filter(hit => storedFields.document(hit.doc).get("agenId") == ofAgent.value)
      .map(hit => {
        val doc = storedFields.document(hit.doc)
        val owner = doc.get("owner")
        val name = doc.get("name")
        val typ = doc.get("type")
        val remarks = doc.get("remarks")
        val appldata = doc.get("appldata")
        val agentId = doc.get("agenId")
        QMFObject(owner, name, typ, remarks, appldata)
      })
