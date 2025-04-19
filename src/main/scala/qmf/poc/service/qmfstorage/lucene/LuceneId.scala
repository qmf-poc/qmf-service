package qmf.poc.service.qmfstorage.lucene

opaque type LuceneId = Int

object LuceneId:
  def apply(id: Int): LuceneId = id
  def apply(id: String): LuceneId = id.hashCode

  extension (id: LuceneId)
    def value: Int = id
    def toString: String = id.toString

  given Conversion[LuceneId, Int] with
    def apply(id: LuceneId): Int = id.value

  given Conversion[String, LuceneId] with
    def apply(string: String): LuceneId = LuceneId(string)
