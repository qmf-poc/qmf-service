package qmf.poc.service.repository.lucene

import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.document.{Document, IntPoint, StoredField, StringField, TextField}
import org.apache.lucene.index.{DirectoryReader, IndexWriter, IndexWriterConfig, Term}
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.search.{IndexSearcher, TermQuery}
import org.apache.lucene.store.ByteBuffersDirectory

//noinspection SimplifyAssertInspection
class LuceneTest extends munit.FunSuite:
  test("the document was written"):
    val analyzer = new StandardAnalyzer
    val index = new ByteBuffersDirectory
    val config = IndexWriterConfig(analyzer)
    val w = new IndexWriter(index, config)

    val doc = new Document()
    doc.add(new IntPoint("id", -1))
    doc.add(new TextField("text payload 1", "payload 1", org.apache.lucene.document.Field.Store.YES))
    doc.add(new TextField("text payload 2", "payload 2", org.apache.lucene.document.Field.Store.YES))
    w.addDocument(doc)
    w.commit()

    val r = DirectoryReader.open(index)
    assertEquals(r.numDocs(), 1)

  test("the document can be searched by int point field id"):
    val analyzer = new StandardAnalyzer
    val index = new ByteBuffersDirectory
    val config = IndexWriterConfig(analyzer)
    val w = new IndexWriter(index, config)

    val doc = new Document()
    doc.add(new IntPoint("id", -1))
    doc.add(new TextField("text payload 1", "payload 1", org.apache.lucene.document.Field.Store.YES))
    doc.add(new TextField("text payload 2", "payload 2", org.apache.lucene.document.Field.Store.YES))
    w.addDocument(doc)
    w.commit()

    val r = DirectoryReader.open(index)
    val sf0 = r.storedFields()

    // Assert the document can be searched by int point field id
    val s = new IndexSearcher(r)
    val query = IntPoint.newExactQuery("id", -1)
    val results = s.search(query, 10)
    val hits = results.scoreDocs
    assertEquals(hits.length, 1)
    val docId = hits(0).doc
    val sf1 = s.storedFields()
    val d1 = sf1.document(docId)
    assertEquals(d1.get("text payload 1"), "payload 1")
    assertEquals(d1.get("text payload 2"), "payload 2")

  test("the document can be searched by string term"):
    val analyzer = new StandardAnalyzer
    val index = new ByteBuffersDirectory
    val config = IndexWriterConfig(analyzer)
    val w = new IndexWriter(index, config)

    val doc = new Document()
    doc.add(new IntPoint("id", -1))
    doc.add(new StringField("text payload 1", "payload 1", org.apache.lucene.document.Field.Store.YES))
    doc.add(new TextField("text payload 2", "payload 2", org.apache.lucene.document.Field.Store.YES))
    w.addDocument(doc)
    w.commit()

    val r = DirectoryReader.open(index)
    val sf0 = r.storedFields()

    // Assert the document can be searched by int point field id
    val s = new IndexSearcher(r)
    val query = new TermQuery(new Term("text payload 1", "payload 1"))
    val results = s.search(query, 10)
    assertEquals(results.totalHits.value, 1L)

    val hits = results.scoreDocs
    val docId = hits(0).doc
    val sf1 = s.storedFields()
    val d1 = sf1.document(docId)
    assertEquals(d1.get("text payload 1"), "payload 1")
    assertEquals(d1.get("text payload 2"), "payload 2")

  test("the document can not be searched by string term against text field"):
    val analyzer = new StandardAnalyzer
    val index = new ByteBuffersDirectory
    val config = IndexWriterConfig(analyzer)
    val w = new IndexWriter(index, config)

    val doc = new Document()
    doc.add(new IntPoint("id", -1))
    doc.add(new TextField("text payload 1", "payload 1", org.apache.lucene.document.Field.Store.YES))
    doc.add(new TextField("text payload 2", "payload 2", org.apache.lucene.document.Field.Store.YES))
    w.addDocument(doc)
    w.commit()

    val r = DirectoryReader.open(index)
    val sf0 = r.storedFields()

    // Assert the document can be searched by int point field id
    val s = new IndexSearcher(r)
    val query = new TermQuery(new Term("text payload 1", "payload 1"))
    val results = s.search(query, 10)
    assertEquals(results.totalHits.value, 0L)

  test("the document can be searched by string term against text field"):
    val analyzer = new StandardAnalyzer
    val index = new ByteBuffersDirectory
    val config = IndexWriterConfig(analyzer)
    val w = new IndexWriter(index, config)

    val doc = new Document()
    doc.add(new IntPoint("id", -1))
    doc.add(new StringField("text payload 1", "payload 1", org.apache.lucene.document.Field.Store.YES))
    doc.add(new TextField("text payload 2", "payload 2", org.apache.lucene.document.Field.Store.YES))
    w.addDocument(doc)
    w.commit()

    val r = DirectoryReader.open(index)
    val sf0 = r.storedFields()

    // Assert the document can be searched by int point field id
    val query = new TermQuery(new Term("text payload 1", "payload 1"))
    val s = new IndexSearcher(r)
    val results = s.search(query, 10)
    assertEquals(results.totalHits.value, 1L)

    val hits = results.scoreDocs
    val docId = hits(0).doc
    val sf1 = s.storedFields()
    val d1 = sf1.document(docId)
    assertEquals(d1.get("text payload 1"), "payload 1")
    assertEquals(d1.get("text payload 2"), "payload 2")

  test("the document can be searched by query parser against text field"):
    val analyzer = new StandardAnalyzer
    val index = new ByteBuffersDirectory
    val config = IndexWriterConfig(analyzer)
    val w = new IndexWriter(index, config)

    val doc = new Document()
    doc.add(new IntPoint("id", -1))
    doc.add(new TextField("field1", "payload 1", org.apache.lucene.document.Field.Store.YES))
    doc.add(new TextField("field2", "something 2", org.apache.lucene.document.Field.Store.YES))
    w.addDocument(doc)
    w.commit()

    val r = DirectoryReader.open(index)
    val sf0 = r.storedFields()

    // Assert the document can be searched by int point field id
    val qs = "field1:payload"
    val q = new QueryParser("field2", analyzer).parse(qs)
    val s = new IndexSearcher(r)
    val results = s.search(q, 10)
    assertEquals(results.totalHits.value, 1L)

    val hits = results.scoreDocs
    val docId = hits(0).doc
    val sf1 = s.storedFields()
    val d1 = sf1.document(docId)
    assertEquals(d1.get("field1"), "payload 1")
    assertEquals(d1.get("field2"), "something 2")

  test("the document can be searched by query parser against combined text field"):
    val analyzer = new StandardAnalyzer
    val index = new ByteBuffersDirectory
    val config = IndexWriterConfig(analyzer)
    val w = new IndexWriter(index, config)

    val doc0 = new Document()
    doc0.add(new IntPoint("id", 0))
    w.addDocument(doc0)
    w.commit()

    val doc = new Document()
    doc.add(new IntPoint("id", -1))
    doc.add(new TextField("record", "payload 1 payload 2", org.apache.lucene.document.Field.Store.YES))
    doc.add(new StoredField("field1", "payload 1"))
    doc.add(new StoredField("field2", "something 2"))
    w.addDocument(doc)
    w.commit()

    val r = DirectoryReader.open(index)
    val sf0 = r.storedFields()

    // Assert the document can be searched by int point field id
    val qs = "1"
    val q = new QueryParser("record", analyzer).parse(qs)
    val s = new IndexSearcher(r)
    val results = s.search(q, 10)
    assertEquals(results.totalHits.value, 1L)

    val hits = results.scoreDocs
    val docId = hits(0).doc
    val sf1 = s.storedFields()
    val d1 = sf1.document(docId)
    assertEquals(d1.get("field1"), "payload 1")
    assertEquals(d1.get("field2"), "something 2")

  test("the document can be searched by *mask*"):
    val analyzer = new StandardAnalyzer
    val index = new ByteBuffersDirectory
    val config = IndexWriterConfig(analyzer)
    val w = new IndexWriter(index, config)

    val doc = new Document()
    doc.add(new IntPoint("id", -1))
    doc.add(new TextField("field1", "payload 1", org.apache.lucene.document.Field.Store.YES))
    doc.add(new TextField("field2", "something 2", org.apache.lucene.document.Field.Store.YES))
    w.addDocument(doc)
    w.commit()

    val r = DirectoryReader.open(index)
    val sf0 = r.storedFields()

    // Assert the document can be searched by int point field id
    val qs = "*yloa*"
    val qp = new QueryParser("field1", analyzer)
    qp.setAllowLeadingWildcard(true)
    val q = qp.parse(qs)
    val s = new IndexSearcher(r)
    val results = s.search(q, 10)
    assertEquals(results.totalHits.value, 1L)

    val hits = results.scoreDocs
    val docId = hits(0).doc
    val sf1 = s.storedFields()
    val d1 = sf1.document(docId)
    assertEquals(d1.get("field1"), "payload 1")
    assertEquals(d1.get("field2"), "something 2")

  test("the document can be searched by *mask* if starts with mask"):
    val analyzer = new StandardAnalyzer
    val index = new ByteBuffersDirectory
    val config = IndexWriterConfig(analyzer)
    val w = new IndexWriter(index, config)

    val doc = new Document()
    doc.add(new IntPoint("id", -1))
    doc.add(new TextField("field1", "payload 1", org.apache.lucene.document.Field.Store.YES))
    doc.add(new TextField("field2", "something 2", org.apache.lucene.document.Field.Store.YES))
    w.addDocument(doc)
    w.commit()

    val r = DirectoryReader.open(index)
    val sf0 = r.storedFields()

    // Assert the document can be searched by int point field id
    val qs = "*payloa*"
    val qp = new QueryParser("field1", analyzer)
    qp.setAllowLeadingWildcard(true)
    val q = qp.parse(qs)
    val s = new IndexSearcher(r)
    val results = s.search(q, 10)
    assertEquals(results.totalHits.value, 1L)

    val hits = results.scoreDocs
    val docId = hits(0).doc
    val sf1 = s.storedFields()
    val d1 = sf1.document(docId)
    assertEquals(d1.get("field1"), "payload 1")
    assertEquals(d1.get("field2"), "something 2")
