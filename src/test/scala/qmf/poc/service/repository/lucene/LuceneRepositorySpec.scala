package qmf.poc.service.repository.lucene

import org.apache.lucene.store.ByteBuffersDirectory
import qmf.poc.service.catalog.{CatalogSnapshot, ObjectData, ObjectDirectory, ObjectRemarks}
import qmf.poc.service.repository.QMFObject.toUTF8
import qmf.poc.service.repository.lucene.LuceneRepositorySpec.test
import qmf.poc.service.repository.{LuceneRepository, QMFObject, RepositoryError}
import zio.ZIO
import zio.test.Assertion.{equalTo, hasAt, hasField}
import zio.test.{Assertion, Spec, ZIOSpecDefault, assert, assertCompletes, assertZIO}

import java.nio.charset.Charset

object LuceneRepositoryFixtures:
  val snapshot1 = CatalogSnapshot(
    Seq(ObjectData("owner1", "name1", "type1", 1, "appldata1".toEBCDIC.getBytes)),
    Seq(ObjectRemarks("owner1", "name1", "type1", "remark1")),
    Seq(ObjectDirectory("owner1", "name1", "type1", "subtype1", 1, "", "", "", "", ""))
  )
  val snapshot2 = CatalogSnapshot(
    Seq(
      ObjectData("owner1", "name1", "type1", 1, "appldata1".toEBCDIC.getBytes),
      ObjectData("owner2", "name2", "type2", 1, "appldata2".toEBCDIC.getBytes)
    ),
    Seq(
      ObjectRemarks("owner1", "name1", "type1", "remark1"),
      ObjectRemarks("owner2", "name2", "type2", "remark2")
    ),
    Seq(
      ObjectDirectory("owner1", "name1", "type1", "subtype1", 1, "", "", "", "", ""),
      ObjectDirectory("owner2", "name2", "type2", "subtype2", 1, "", "", "", "", "")
    )
  )

  extension (s: String) def toEBCDIC: String = new String(s.getBytes, Charset.forName("IBM1047"))

import qmf.poc.service.repository.lucene.LuceneRepositoryFixtures.{snapshot1, snapshot2}
object LuceneRepositorySpec extends ZIOSpecDefault:
  def spec: Spec[Any, RepositoryError] = suite("LuceneRepository test")(
    suite("persisting")(
      test("should persist a document without error") {
        // Arrange
        val luceneRepository = LuceneRepository(new ByteBuffersDirectory())
        // Act
        for {
          _ <- luceneRepository.persist(QMFObject("owner", "test", "type", "appldata"))
          // Assert
        } yield assertCompletes
      },
      test("should persist a snapshot with one document without error") {
        // Arrange
        val luceneRepository = LuceneRepository(new ByteBuffersDirectory())
        // Act
        val zio = for {
          c <- luceneRepository.load(snapshot1)
          // Assert
        } yield c
        assertZIO(zio)(Assertion.anything && Assertion.equalTo(1))
      },
      test("should persist a snapshot with 2 documents without error") {
        // Arrange
        val luceneRepository = LuceneRepository(new ByteBuffersDirectory())
        // Act
        for {
          _ <- luceneRepository.load(snapshot2)
          // Assert
        } yield assertCompletes
      }
    ),
    suite("retrieving")(
      test("should find all documents with empty query string") {
        // Arrange
        val luceneRepository = LuceneRepository(new ByteBuffersDirectory())
        // Act
        val result = for {
          _ <- luceneRepository.load(snapshot2)
          v <- luceneRepository.retrieve("")
          // Assert
        } yield v
        // assertZIO(result)(Assertion.equalTo(snapshot2))
        assertZIO(result)(
          hasField[Seq[QMFObject], Int]("length", r => r.length, equalTo(2)) &&
            hasAt(0)(
              hasField[QMFObject, String]("owner", r => r.owner, equalTo("owner1")) &&
                hasField("typ", r => r.typ, equalTo("type1")) &&
                hasField("name", r => r.name, equalTo("name1")) &&
                hasField("appldata", r => r.applData, equalTo(snapshot2.objectData.head.appldata.toUTF8)) &&
                hasField("name", r => r.owner, equalTo("owner1"))
            ) &&
            hasAt(1)(
              hasField[QMFObject, String]("owner", r => r.owner, equalTo("owner2")) &&
                hasField("typ", r => r.typ, equalTo("type2")) &&
                hasField("name", r => r.name, equalTo("name2")) &&
                hasField("appldata", r => r.applData, equalTo(snapshot2.objectData(1).appldata.toUTF8)) &&
                hasField("name", r => r.owner, equalTo("owner2"))
            )
        )
      },
      test("should find a document by prefix owner") {
        // Arrange
        val luceneRepository = LuceneRepository(new ByteBuffersDirectory())
        // Act
        for {
          _ <- luceneRepository.load(snapshot2)
          v <- luceneRepository.retrieve("~owne")
          // Assert
        } yield assert(v)(Assertion.equalTo(Seq(QMFObject("owner", "test", "type", "appldata"))))
      },
      test("should find a document by fusion") {
        // Arrange
        val luceneRepository = LuceneRepository(new ByteBuffersDirectory())
        // Act
        for {
          _ <- luceneRepository.persist(QMFObject("owner", "test", "type", "appldata"))
          v <- luceneRepository.retrieve("test")
          // Assert
        } yield assert(v)(Assertion.equalTo(Seq(QMFObject("owner", "test", "type", "appldata"))))
      },
      test("should handle empty search") {
        // Arrange
        val luceneRepository = LuceneRepository(new ByteBuffersDirectory())
        // Act
        for {
          _ <- luceneRepository.persist(QMFObject("owner", "test", "type", "appldata"))
          v <- luceneRepository.retrieve("")
          // Assert
        } yield assert(v)(Assertion.equalTo(Seq()))
      },
      test("should handle short search") {
        // Arrange
        val luceneRepository = LuceneRepository(new ByteBuffersDirectory())
        // Act
        for {
          _ <- luceneRepository.persist(QMFObject("owner", "test", "type", "appldata"))
          v <- luceneRepository.retrieve("o")
          // Assert
        } yield assert(v)(Assertion.equalTo(Seq(QMFObject("owner", "test", "type", "appldata"))))
      },
      test("should not find an irrelevant document") {
        // Arrange
        val luceneRepository = LuceneRepository(new ByteBuffersDirectory())
        // Act
        for {
          _ <- luceneRepository.persist(QMFObject("owner", "test", "type", "appldata"))
          v <- luceneRepository.retrieve("aaaa")
          // Assert
        } yield assert(v)(Assertion.equalTo(Seq()))
      }
    )
  )
