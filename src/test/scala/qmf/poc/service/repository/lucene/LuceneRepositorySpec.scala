package qmf.poc.service.repository.lucene

import org.apache.lucene.store.ByteBuffersDirectory
import qmf.poc.service.repository.{LuceneRepository, QMFObject, RepositoryError}
import zio.test.Assertion.equalTo
import zio.test.{assert, Assertion, Spec, ZIOSpecDefault, assertCompletes, assertCompletesZIO}

object LuceneRepositorySpec extends ZIOSpecDefault:
  def spec: Spec[Any, RepositoryError] = suite("LuceneRepository test")(
    test("should persist a document without error") {
      // Arrange
      val luceneRepository = LuceneRepository(new ByteBuffersDirectory())
      // Act
      for {
        _ <- luceneRepository.persist(QMFObject("owner", "test", "type"))
        // Assert
      } yield assertCompletes
    },
    test("should find a document by exact owner") {
      // Arrange
      val luceneRepository = LuceneRepository(new ByteBuffersDirectory())
      // Act
      for {
        _ <- luceneRepository.persist(QMFObject("owner", "test", "type"))
        v <- luceneRepository.retrieve("owner:owner")
        // Assert
      } yield   assert(v)(Assertion.equalTo(Seq(QMFObject("owner", "test", "type"))))
    }
  )
