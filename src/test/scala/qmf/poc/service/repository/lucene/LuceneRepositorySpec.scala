package qmf.poc.service.repository.lucene

import org.apache.lucene.store.ByteBuffersDirectory
import qmf.poc.service.repository.lucene.LuceneRepositorySpec.test
import qmf.poc.service.repository.{LuceneRepository, QMFObject, RepositoryError}
import zio.test.Assertion.equalTo
import zio.test.{Assertion, Spec, ZIOSpecDefault, assert, assertCompletes, assertCompletesZIO}

object LuceneRepositorySpec extends ZIOSpecDefault:
  def spec: Spec[Any, RepositoryError] = suite("LuceneRepository test")(
    test("should persist a document without error") {
      // Arrange
      val luceneRepository = LuceneRepository(new ByteBuffersDirectory())
      // Act
      for {
        _ <- luceneRepository.persist(QMFObject("owner", "test", "type", "appldata"))
        // Assert
      } yield assertCompletes
    },
    test("should find a document by prefix owner") {
      // Arrange
      val luceneRepository = LuceneRepository(new ByteBuffersDirectory())
      // Act
      for {
        _ <- luceneRepository.persist(QMFObject("owner", "test", "type", "appldata"))
        v <- luceneRepository.retrieve("owne")
        // Assert
      } yield   assert(v)(Assertion.equalTo(Seq(QMFObject("owner", "test", "type", "appldata"))))
    },
    test("should find a document by fusion") {
      // Arrange
      val luceneRepository = LuceneRepository(new ByteBuffersDirectory())
      // Act
      for {
        _ <- luceneRepository.persist(QMFObject("owner", "test", "type", "appldata"))
        v <- luceneRepository.retrieve("test")
        // Assert
      } yield   assert(v)(Assertion.equalTo(Seq(QMFObject("owner", "test", "type", "appldata"))))
    },
    test("should handle empty search") {
      // Arrange
      val luceneRepository = LuceneRepository(new ByteBuffersDirectory())
      // Act
      for {
        _ <- luceneRepository.persist(QMFObject("owner", "test", "type", "appldata"))
        v <- luceneRepository.retrieve("")
        // Assert
      } yield   assert(v)(Assertion.equalTo(Seq()))
    },
    test("should handle short search") {
      // Arrange
      val luceneRepository = LuceneRepository(new ByteBuffersDirectory())
      // Act
      for {
        _ <- luceneRepository.persist(QMFObject("owner", "test", "type", "appldata"))
        v <- luceneRepository.retrieve("o")
        // Assert
      } yield   assert(v)(Assertion.equalTo(Seq(QMFObject("owner", "test", "type", "appldata"))))
    },
    test("should not find an irrelevant document") {
      // Arrange
      val luceneRepository = LuceneRepository(new ByteBuffersDirectory())
      // Act
      for {
        _ <- luceneRepository.persist(QMFObject("owner", "test", "type", "appldata"))
        v <- luceneRepository.retrieve("aaaa")
        // Assert
      } yield   assert(v)(Assertion.equalTo(Seq()))
    }
  )
