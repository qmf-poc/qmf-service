package qmf.poc.service.repository

import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.document.*
import org.apache.lucene.index.{DirectoryReader, IndexWriter, IndexWriterConfig}
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.store.Directory
import qmf.poc.service.repository.*
import zio.{CanFail, IO, ZIO}

type LuceneId = Int

case class QMFObject(owner: String, name: String, typ: String)

class RepositoryError(val th: Throwable) extends Exception

class LuceneRepository(directory: Directory):
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

  def persist(qmfObject: QMFObject): IO[RepositoryError, Unit] = ZIO.blocking {
    for
      exists <- has(luceneId(qmfObject))
      _ <- ZIO.unless(exists)(add(qmfObject))
    yield ()
  }

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
