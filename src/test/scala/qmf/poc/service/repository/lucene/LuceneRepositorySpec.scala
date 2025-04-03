package qmf.poc.service.repository.lucene
import org.apache.lucene.store.ByteBuffersDirectory
import qmf.poc.service.catalog.{CatalogSnapshot, given}
import qmf.poc.service.repository.lucene.LuceneRepositorySpec.test
import qmf.poc.service.repository.{LuceneRepository, QMFObject, RepositoryError, RepositoryErrorObjectNotFound}
import zio.{Exit, IO, Scope, ZIO}
import zio.test.Assertion.{anything, equalTo, fails, hasAt, hasField, isSubtype}
import zio.test.{Assertion, Spec, TestEnvironment, ZIOSpecDefault, assert, assertCompletes, assertTrue, assertZIO}
import zio.json.given

import scala.io.Source

object LuceneRepositoryFixtures:
  val sampleZIO: IO[String, CatalogSnapshot] = ZIO.fromEither {
    val json = Source.fromResource("sample.json").getLines().mkString("\n")
    json.fromJson[CatalogSnapshot]
  }

object LuceneRepositorySpec extends ZIOSpecDefault:

  import qmf.poc.service.repository.lucene.LuceneRepositoryFixtures.sampleZIO

  def spec: Spec[TestEnvironment & Scope, Any] = suite("LuceneRepository test")(
    test("sample created") {
      assertZIO(sampleZIO)(Assertion.isSubtype[CatalogSnapshot](anything))
    },
    test("query for all") {
      val luceneRepository = LuceneRepository(new ByteBuffersDirectory())
      val result = for {
        sample <- sampleZIO
        c <- luceneRepository.load(sample)
        x <- luceneRepository.query("")
        r = c == x.length // sample.qmfObjects.length
      } yield r
      assertZIO(result)(Assertion.isTrue)
      // assertZIO(result)(Assertion.isSubtype[CatalogSnapshot](anything))
    }
  )
/*
val snapshot1: CatalogSnapshot = CatalogSnapshot(
  Seq(ObjectData("owner1", "name1", "type1", 1, "appldata1".toEBCDIC)),
  Seq(ObjectRemarks("owner1", "name1", "type1", "remark1")),
  Seq(ObjectDirectory("owner1", "name1", "type1", "subtype1", 1, "", "", "", "", ""))
)
val snapshot2: CatalogSnapshot = CatalogSnapshot(
  Seq(
    ObjectData("owner1", "name1", "type1", 1, "appldata1".toEBCDIC),
    ObjectData("owner2", "name2", "type2", 1, "appldata2".toEBCDIC)
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
val snapshot3: CatalogSnapshot = CatalogSnapshot(
  Seq(
    ObjectData("1owner1", "1name1", "1type1", 1, "1appldata1".toEBCDIC),
    ObjectData("2owner2", "2name2", "2type2", 1, "2appldata2".toEBCDIC),
    ObjectData("3owner3", "3name3", "3type3", 1, "3appldata3".toEBCDIC)
  ),
  Seq(
    ObjectRemarks("1owner1", "1name1", "1type1", "1remark1"),
    ObjectRemarks("2owner2", "2name2", "2type2", "2remark2"),
    ObjectRemarks("3owner3", "3name3", "3type3", "3remark3")
  ),
  Seq(
    ObjectDirectory("1owner1", "1name1", "1type1", "1subtype1", 1, "", "", "", "", ""),
    ObjectDirectory("2owner2", "2name2", "2type2", "2subtype2", 1, "", "", "", "", ""),
    ObjectDirectory("3owner3", "3name3", "3type3", "3subtype3", 1, "", "", "", "", "")
  )
)
val snapshot3with2commonOwners: CatalogSnapshot = CatalogSnapshot(
  Seq(
    ObjectData("1owner1", "1name1", "1type1", 1, "1appldata1".toEBCDIC),
    ObjectData("2owner2", "2name2", "2type2", 1, "2appldata2".toEBCDIC),
    ObjectData("3other3", "3name3", "3type3", 1, "3appldata3".toEBCDIC)
  ),
  Seq(
    ObjectRemarks("1owner1", "1name1", "1type1", "1remark1"),
    ObjectRemarks("2owner2", "2name2", "2type2", "2remark2"),
    ObjectRemarks("3other3", "3name3", "3type3", "3remark3")
  ),
  Seq(
    ObjectDirectory("1owner1", "1name1", "1type1", "1subtype1", 1, "", "", "", "", ""),
    ObjectDirectory("2owner2", "2name2", "2type2", "2subtype2", 1, "", "", "", "", ""),
    ObjectDirectory("3other3", "3name3", "3type3", "3subtype3", 1, "", "", "", "", "")
  )
)

extension (s: String) def toEBCDIC: Array[Byte] = s.getBytes(Charset.forName("IBM1047"))

import qmf.poc.service.repository.lucene.LuceneRepositoryFixtures.{snapshot1, snapshot2, snapshot3, snapshot3with2commonOwners}
object LuceneRepositorySpec extends ZIOSpecDefault:
def spec: Spec[Any, RepositoryError] = suite("LuceneRepository test")(
  suite("encoding")(
    test("appldata comparable with UTF-8") {
      val odValue = snapshot3.objectData.head
      val orValue = snapshot3.objectRemarks.head
      val doc = QMFObject(odValue.owner, orValue.name, orValue.`type`, orValue.remarks, odValue.appldata)
      assert(doc.applData)(Assertion.equalTo("1appldata1"))
    }
  ),
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
        v <- luceneRepository.query("")
      } yield v
      // Assert
      assertZIO(result)(
        hasField[Seq[QMFObject], Int]("length", r => r.length, equalTo(2)) &&
          hasAt(0)(
            hasField[QMFObject, String]("owner", r => r.owner, equalTo("owner1")) &&
              hasField("typ", r => r.typ, equalTo("type1")) &&
              hasField("name", r => r.name, equalTo("name1")) &&
              hasField("appldata", r => r.applData, equalTo(snapshot2.objectData.head.appldata.toUTF8)) &&
              hasField("owner", r => r.owner, equalTo("owner1"))
          ) &&
          hasAt(1)(
            hasField[QMFObject, String]("owner", r => r.owner, equalTo("owner2")) &&
              hasField("typ", r => r.typ, equalTo("type2")) &&
              hasField("name", r => r.name, equalTo("name2")) &&
              hasField("appldata", r => r.applData, equalTo(snapshot2.objectData(1).appldata.toUTF8)) &&
              hasField("owner", r => r.owner, equalTo("owner2"))
          )
      )
    },
    test("should find all documents with empty query string #2") {
      // Arrange
      val luceneRepository = LuceneRepository(new ByteBuffersDirectory())
      // Act
      val result = for {
        _ <- luceneRepository.load(snapshot3with2commonOwners)
        v <- luceneRepository.query("")
      } yield v
      // Assert
      assertZIO(result)(
        hasField[Seq[QMFObject], Int]("length", r => r.length, equalTo(3)) &&
          hasAt(0)(
            hasField[QMFObject, String]("owner", r => r.owner, equalTo("1owner1")) &&
              hasField("typ", r => r.typ, equalTo("1type1")) &&
              hasField("name", r => r.name, equalTo("1name1")) &&
              hasField("appldata", r => r.applData, equalTo(snapshot3with2commonOwners.objectData.head.appldata.toUTF8)) &&
              hasField("owner", r => r.owner, equalTo("1owner1"))
          ) &&
          hasAt(1)(
            hasField[QMFObject, String]("owner", r => r.owner, equalTo("2owner2")) &&
              hasField("typ", r => r.typ, equalTo("2type2")) &&
              hasField("name", r => r.name, equalTo("2name2")) &&
              hasField("appldata", r => r.applData, equalTo(snapshot3with2commonOwners.objectData(1).appldata.toUTF8)) &&
              hasField("owner", r => r.owner, equalTo("2owner2"))
          ) &&
          hasAt(2)(
            hasField[QMFObject, String]("owner", r => r.owner, equalTo("3other3")) &&
              hasField("typ", r => r.typ, equalTo("3type3")) &&
              hasField("name", r => r.name, equalTo("3name3")) &&
              hasField("appldata", r => r.applData, equalTo(snapshot3with2commonOwners.objectData(2).appldata.toUTF8)) &&
              hasField("owner", r => r.owner, equalTo("3other3"))
          )
      )
    },
    test("should find all documents with short query string") {
      // Arrange
      val luceneRepository = LuceneRepository(new ByteBuffersDirectory())
      // Act
      val result = for {
        _ <- luceneRepository.load(snapshot3)
        v <- luceneRepository.query("o")
      } yield v
      // Assert
      assertZIO(result)(
        hasField[Seq[QMFObject], Int]("length", r => r.length, equalTo(3)) &&
          hasAt(0)(
            hasField[QMFObject, String]("owner", r => r.owner, equalTo("1owner1")) &&
              hasField("typ", r => r.typ, equalTo("1type1")) &&
              hasField("name", r => r.name, equalTo("1name1")) &&
              hasField("appldata", r => r.applData, equalTo(snapshot3.objectData.head.appldata.toUTF8)) &&
              hasField("owner", r => r.owner, equalTo("1owner1"))
          ) &&
          hasAt(1)(
            hasField[QMFObject, String]("owner", r => r.owner, equalTo("2owner2")) &&
              hasField("typ", r => r.typ, equalTo("2type2")) &&
              hasField("name", r => r.name, equalTo("2name2")) &&
              hasField("appldata", r => r.applData, equalTo(snapshot3.objectData(1).appldata.toUTF8)) &&
              hasField("owner", r => r.owner, equalTo("2owner2"))
          ) && hasAt(2)(
            hasField[QMFObject, String]("owner", r => r.owner, equalTo("3owner3")) &&
              hasField("typ", r => r.typ, equalTo("3type3")) &&
              hasField("name", r => r.name, equalTo("3name3")) &&
              hasField("appldata", r => r.applData, equalTo(snapshot3.objectData(2).appldata.toUTF8)) &&
              hasField("owner", r => r.owner, equalTo("3owner3"))
          )
      )
    },
    test("should find a document by owner's part") {
      // Arrange
      val luceneRepository = LuceneRepository(new ByteBuffersDirectory())
      // Act
      val result = for {
        _ <- luceneRepository.load(snapshot3with2commonOwners)
        v <- luceneRepository.query("own")
      } yield v
      // Assert
      assertZIO(result)(
        hasField[Seq[QMFObject], Int]("length", r => r.length, equalTo(2)) &&
          hasAt(0)(
            hasField[QMFObject, String]("owner", r => r.owner, equalTo("1owner1")) &&
              hasField("typ", r => r.typ, equalTo("1type1")) &&
              hasField("name", r => r.name, equalTo("1name1")) &&
              hasField("appldata", r => r.applData, equalTo(snapshot3.objectData.head.appldata.toUTF8)) &&
              hasField("owner", r => r.owner, equalTo("1owner1"))
          ) &&
          hasAt(1)(
            hasField[QMFObject, String]("owner", r => r.owner, equalTo("2owner2")) &&
              hasField("typ", r => r.typ, equalTo("2type2")) &&
              hasField("name", r => r.name, equalTo("2name2")) &&
              hasField("appldata", r => r.applData, equalTo(snapshot3.objectData(1).appldata.toUTF8)) &&
              hasField("owner", r => r.owner, equalTo("2owner2"))
          )
      )
    },
    test("should not find an irrelevant document") {
      // Arrange
      val luceneRepository = LuceneRepository(new ByteBuffersDirectory())
      // Act
      val result = for {
        _ <- luceneRepository.load(snapshot3with2commonOwners)
        v <- luceneRepository.query("xyz")
      } yield v
      // Assert
      assertZIO(result)(Assertion.equalTo(Seq()))
    }
  ),
  test("should find a document by remark") {
    // Arrange
    val luceneRepository = LuceneRepository(new ByteBuffersDirectory())
    // Act
    val result = for {
      _ <- luceneRepository.load(snapshot3with2commonOwners)
      v <- luceneRepository.query("remarks:1remark1")
    } yield v
    // Assert
    assertZIO(result)(
      hasField[Seq[QMFObject], Int]("length", r => r.length, equalTo(1)) &&
        hasAt(0)(
          hasField[QMFObject, String]("owner", r => r.owner, equalTo("1owner1")) &&
            hasField("typ", r => r.typ, equalTo("1type1")) &&
            hasField("name", r => r.name, equalTo("1name1")) &&
            hasField("remarks", r => r.remarks, equalTo("1remark1")) &&
            hasField("appldata", r => r.applData, equalTo(snapshot3.objectData.head.appldata.toUTF8)) &&
            hasField("owner", r => r.owner, equalTo("1owner1"))
        )
    )
  },
  test("should find a document by remark's prefix") {
    // Arrange
    val luceneRepository = LuceneRepository(new ByteBuffersDirectory())
    // Act
    val result = for {
      _ <- luceneRepository.load(snapshot3with2commonOwners)
      v <- luceneRepository.query("remarks:1remark*")
    } yield v
    // Assert
    assertZIO(result)(
      hasField[Seq[QMFObject], Int]("length", r => r.length, equalTo(1)) &&
        hasAt(0)(
          hasField[QMFObject, String]("owner", r => r.owner, equalTo("1owner1")) &&
            hasField("typ", r => r.typ, equalTo("1type1")) &&
            hasField("name", r => r.name, equalTo("1name1")) &&
            hasField("remarks", r => r.remarks, equalTo("1remark1")) &&
            hasField("appldata", r => r.applData, equalTo(snapshot3.objectData.head.appldata.toUTF8)) &&
            hasField("owner", r => r.owner, equalTo("1owner1"))
        )
    )
  },
  test("should find a document by remark's suffix") {
    // Arrange
    val luceneRepository = LuceneRepository(new ByteBuffersDirectory())
    // Act
    val result = for {
      _ <- luceneRepository.load(snapshot3with2commonOwners)
      v <- luceneRepository.query("remarks:*remark1")
    } yield v
    // Assert
    assertZIO(result)(
      hasField[Seq[QMFObject], Int]("length", r => r.length, equalTo(1)) &&
        hasAt(0)(
          hasField[QMFObject, String]("owner", r => r.owner, equalTo("1owner1")) &&
            hasField("typ", r => r.typ, equalTo("1type1")) &&
            hasField("name", r => r.name, equalTo("1name1")) &&
            hasField("remarks", r => r.remarks, equalTo("1remark1")) &&
            hasField("appldata", r => r.applData, equalTo(snapshot3.objectData.head.appldata.toUTF8)) &&
            hasField("owner", r => r.owner, equalTo("1owner1"))
        )
    )
  },
  test("should find a document by remark's part") {
    // Arrange
    val luceneRepository = LuceneRepository(new ByteBuffersDirectory())
    // Act
    val result = for {
      _ <- luceneRepository.load(snapshot3with2commonOwners)
      v <- luceneRepository.query("remarks:*remark*")
    } yield v
    // Assert
    assertZIO(result)(
      hasField[Seq[QMFObject], Int]("length", r => r.length, equalTo(3)) &&
        hasAt(0)(
          hasField[QMFObject, String]("owner", r => r.owner, equalTo("1owner1")) &&
            hasField("typ", r => r.typ, equalTo("1type1")) &&
            hasField("name", r => r.name, equalTo("1name1")) &&
            hasField("remarks", r => r.remarks, equalTo("1remark1")) &&
            hasField("appldata", r => r.applData, equalTo(snapshot3.objectData.head.appldata.toUTF8)) &&
            hasField("owner", r => r.owner, equalTo("1owner1"))
        )
    )
  },
  test("should find a document by appldata") {
    // Arrange
    val luceneRepository = LuceneRepository(new ByteBuffersDirectory())
    // Act
    val result = for {
      _ <- luceneRepository.load(snapshot3with2commonOwners)
      v <- luceneRepository.query("appldata:1appldata1")
    } yield v
    // Assert
    assertZIO(result)(
      hasField[Seq[QMFObject], Int]("length", r => r.length, equalTo(1)) &&
        hasAt(0)(
          hasField[QMFObject, String]("owner", r => r.owner, equalTo("1owner1")) &&
            hasField("typ", r => r.typ, equalTo("1type1")) &&
            hasField("name", r => r.name, equalTo("1name1")) &&
            hasField("remarks", r => r.remarks, equalTo("1remark1")) &&
            hasField("appldata", r => r.applData, equalTo(snapshot3.objectData.head.appldata.toUTF8)) &&
            hasField("owner", r => r.owner, equalTo("1owner1"))
        )
    )
  },
  test("should find a document by appldata's prefix") {
    // Arrange
    val luceneRepository = LuceneRepository(new ByteBuffersDirectory())
    // Act
    val result = for {
      _ <- luceneRepository.load(snapshot3with2commonOwners)
      v <- luceneRepository.query("appldata:1appldata*")
    } yield v
    // Assert
    assertZIO(result)(
      hasField[Seq[QMFObject], Int]("length", r => r.length, equalTo(1)) &&
        hasAt(0)(
          hasField[QMFObject, String]("owner", r => r.owner, equalTo("1owner1")) &&
            hasField("typ", r => r.typ, equalTo("1type1")) &&
            hasField("name", r => r.name, equalTo("1name1")) &&
            hasField("remarks", r => r.remarks, equalTo("1remark1")) &&
            hasField("appldata", r => r.applData, equalTo(snapshot3.objectData.head.appldata.toUTF8)) &&
            hasField("owner", r => r.owner, equalTo("1owner1"))
        )
    )
  },
  test("should find a document by appldata's suffix") {
    // Arrange
    val luceneRepository = LuceneRepository(new ByteBuffersDirectory())
    // Act
    val result = for {
      _ <- luceneRepository.load(snapshot3with2commonOwners)
      v <- luceneRepository.query("appldata:*appldata1")
    } yield v
    // Assert
    assertZIO(result)(
      hasField[Seq[QMFObject], Int]("length", r => r.length, equalTo(1)) &&
        hasAt(0)(
          hasField[QMFObject, String]("owner", r => r.owner, equalTo("1owner1")) &&
            hasField("typ", r => r.typ, equalTo("1type1")) &&
            hasField("name", r => r.name, equalTo("1name1")) &&
            hasField("remarks", r => r.remarks, equalTo("1remark1")) &&
            hasField("appldata", r => r.applData, equalTo(snapshot3.objectData.head.appldata.toUTF8)) &&
            hasField("owner", r => r.owner, equalTo("1owner1"))
        )
    )
  },
  test("should find a document by appldata's part") {
    // Arrange
    val luceneRepository = LuceneRepository(new ByteBuffersDirectory())
    // Act
    val result = for {
      _ <- luceneRepository.load(snapshot3with2commonOwners)
      v <- luceneRepository.query("appldata:*appldata*")
    } yield v
    // Assert
    assertZIO(result)(
      hasField[Seq[QMFObject], Int]("length", r => r.length, equalTo(3)) &&
        hasAt(0)(
          hasField[QMFObject, String]("owner", r => r.owner, equalTo("1owner1")) &&
            hasField("typ", r => r.typ, equalTo("1type1")) &&
            hasField("name", r => r.name, equalTo("1name1")) &&
            hasField("remarks", r => r.remarks, equalTo("1remark1")) &&
            hasField("appldata", r => r.applData, equalTo(snapshot3.objectData.head.appldata.toUTF8)) &&
            hasField("owner", r => r.owner, equalTo("1owner1"))
        )
    )
  },
  test("should find a document by its id") {
    // Arrange
    val luceneRepository = LuceneRepository(new ByteBuffersDirectory())
    // Act
    val result = for {
      _ <- luceneRepository.load(snapshot3with2commonOwners)
      v <- luceneRepository.get("1owner1-1name1-1type1")
    } yield v
    // Assert
    assertZIO(result)(
      hasField[QMFObject, String]("owner", r => r.owner, equalTo("1owner1")) &&
        hasField("typ", r => r.typ, equalTo("1type1")) &&
        hasField("name", r => r.name, equalTo("1name1")) &&
        hasField("remarks", r => r.remarks, equalTo("1remark1")) &&
        hasField("appldata", r => r.applData, equalTo(snapshot3.objectData.head.appldata.toUTF8))
    )
  },
  test("should not find a document by wrong id") {
    // Arrange
    val luceneRepository = LuceneRepository(new ByteBuffersDirectory())
    // Act
    for {
      _ <- luceneRepository.load(snapshot3with2commonOwners)
      exit <- luceneRepository.get("1owner1+1name1+1type1").exit
    } yield assert(exit)(fails(isSubtype[RepositoryErrorObjectNotFound](anything)))
  }
)
 */
