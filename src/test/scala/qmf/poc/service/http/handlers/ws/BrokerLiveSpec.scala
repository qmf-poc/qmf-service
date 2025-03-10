package qmf.poc.service.http.handlers.ws

import qmf.poc.service.catalog.CatalogSnapshot
import qmf.poc.service.repository.lucene.LuceneRepositorySpec.test
import qmf.poc.service.repository.{QMFObject, Repository, RepositoryError}
import zio.ZIO.service
import zio.test.{Assertion, Spec, ZIOSpecDefault, assert, assertCompletes, assertTrue, assertZIO}
import zio.{IO, Layer, Queue, Ref, ULayer, ZLayer}

object Mock:
  val repositoryLive: Repository = new Repository:
    def load(snapshot: CatalogSnapshot): IO[RepositoryError, Int] = ???

    def persist(qmfObject: QMFObject): IO[RepositoryError, Unit] = ???

    def query(queryString: String): IO[RepositoryError, Seq[QMFObject]] = ???

    override def get(id: String): IO[RepositoryError, QMFObject] = ???

  val repositoryLayer: ULayer[Repository] = ZLayer.fromFunction(() => repositoryLive)
  val brokerQueueLayer: ULayer[Queue[OutgoingMessage]] = ZLayer(Queue.sliding[OutgoingMessage](100))
  val brokerLayer: ULayer[Broker] = (repositoryLayer ++ brokerQueueLayer) >>> BrokerLive.layer

object BrokerLiveSpec extends ZIOSpecDefault:
  def spec =
    suite("Broker tests")(
      test("updating ref") {
        for {
          r <- Ref.make(0)
          _ <- r.update(_ + 1)
          v <- r.get
        } yield assert(v)(Assertion.equalTo(1))
      },
      test("should be instantiated") {
        for {
          broker <- service[Broker]
          queue <- service[Queue[OutgoingMessage]]
          _ <- broker.handle(Ping("test"))
          om <- queue.take
        } yield assert(om)(Assertion.equalTo(ReplyPong("test received")))
      }
    ).provide(Mock.brokerLayer, Mock.brokerQueueLayer)
