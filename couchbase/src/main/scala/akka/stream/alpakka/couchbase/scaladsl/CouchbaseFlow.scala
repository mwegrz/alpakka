/*
 * Copyright (C) 2016-2019 Lightbend Inc. <http://www.lightbend.com>
 */

package akka.stream.alpakka.couchbase.scaladsl
import akka.NotUsed
import akka.stream.alpakka.couchbase.impl.Setup
import akka.stream.alpakka.couchbase._
import akka.stream.scaladsl.Flow
import com.couchbase.client.java.document.{Document, JsonDocument}

/**
 * Scala API: Factory methods for Couchbase flows.
 */
object CouchbaseFlow {

  /**
   * Create a flow to query Couchbase for by `id` and emit [[com.couchbase.client.java.document.JsonDocument JsonDocument]]s.
   */
  def fromId(sessionSettings: CouchbaseSessionSettings, bucketName: String): Flow[String, JsonDocument, NotUsed] =
    Setup
      .flow { materializer => _ =>
        val session = CouchbaseSessionRegistry(materializer.system).sessionFor(sessionSettings, bucketName)
        Flow[String]
          .mapAsync(1)(id => session.flatMap(_.get(id /* timeout? */ ))(materializer.system.dispatcher))
          .collect { case Some(doc) => doc }
      }
      .mapMaterializedValue(_ => NotUsed)

  /**
   * Create a flow to query Couchbase for by `id` and emit documents of the given class.
   */
  def fromId[T <: Document[_]](sessionSettings: CouchbaseSessionSettings,
                               bucketName: String,
                               target: Class[T]): Flow[String, T, NotUsed] =
    Setup
      .flow { materializer => _ =>
        val session = CouchbaseSessionRegistry(materializer.system).sessionFor(sessionSettings, bucketName)
        Flow[String]
          .mapAsync(1)(id => session.flatMap(_.get(id /* timeout? */, target))(materializer.system.dispatcher))
          .collect { case Some(doc) => doc }
      }
      .mapMaterializedValue(_ => NotUsed)

  /**
   * Create a flow to update or insert a Couchbase [[com.couchbase.client.java.document.JsonDocument JsonDocument]].
   */
  def upsert(sessionSettings: CouchbaseSessionSettings,
             writeSettings: CouchbaseWriteSettings,
             bucketName: String): Flow[JsonDocument, JsonDocument, NotUsed] =
    Setup
      .flow { materializer => _ =>
        val session = CouchbaseSessionRegistry(materializer.system).sessionFor(sessionSettings, bucketName)
        Flow[JsonDocument]
          .mapAsync(writeSettings.parallelism)(
            doc => session.flatMap(_.upsert(doc, writeSettings))(materializer.system.dispatcher)
          )
      }
      .mapMaterializedValue(_ => NotUsed)

  /**
   * Create a flow to update or insert a Couchbase document of the given class.
   */
  def upsertDoc[T <: Document[_]](sessionSettings: CouchbaseSessionSettings,
                                  writeSettings: CouchbaseWriteSettings,
                                  bucketName: String): Flow[T, T, NotUsed] =
    Setup
      .flow { materializer => _ =>
        val session = CouchbaseSessionRegistry(materializer.system).sessionFor(sessionSettings, bucketName)
        Flow[T]
          .mapAsync(writeSettings.parallelism)(
            doc => session.flatMap(_.upsertDoc(doc, writeSettings))(materializer.system.dispatcher)
          )
      }
      .mapMaterializedValue(_ => NotUsed)

  /**
   * Create a flow to update or insert a Couchbase document of the given class and emit a result so that write failures
   * can be handled in-stream.
   */
  def upsertDocWithResult[T <: Document[_]](sessionSettings: CouchbaseSessionSettings,
                                            writeSettings: CouchbaseWriteSettings,
                                            bucketName: String): Flow[T, CouchbaseWriteResult[T], NotUsed] =
    Setup
      .flow { materializer => _ =>
        val session = CouchbaseSessionRegistry(materializer.system).sessionFor(sessionSettings, bucketName)
        Flow[T]
          .mapAsync(writeSettings.parallelism)(
            doc => {
              implicit val executor = materializer.system.dispatcher
              session
                .flatMap(_.upsertDoc(doc, writeSettings))
                .map(_ => CouchbaseWriteSuccess(doc))
                .recover {
                  case exception => CouchbaseWriteFailure(doc, exception)
                }
            }
          )
      }
      .mapMaterializedValue(_ => NotUsed)

  /**
   * Create a flow to delete documents from Couchbase by `id`. Emits the same `id`.
   */
  def delete(sessionSettings: CouchbaseSessionSettings,
             writeSettings: CouchbaseWriteSettings,
             bucketName: String): Flow[String, String, NotUsed] =
    Setup
      .flow { materializer => _ =>
        val session = CouchbaseSessionRegistry(materializer.system).sessionFor(sessionSettings, bucketName)
        Flow[String]
          .mapAsync(writeSettings.parallelism)(
            id => {
              implicit val executor = materializer.system.dispatcher
              session
                .flatMap(_.remove(id, writeSettings))
                .map(_ => id)
            }
          )
      }
      .mapMaterializedValue(_ => NotUsed)

  /**
   * Create a flow to delete documents from Couchbase by `id` and emit operation outcome containing the same `id`.
   */
  def deleteWithResult(sessionSettings: CouchbaseSessionSettings,
                       writeSettings: CouchbaseWriteSettings,
                       bucketName: String): Flow[String, CouchbaseDeleteResult, NotUsed] =
    Setup
      .flow { materializer => _ =>
        val session = CouchbaseSessionRegistry(materializer.system).sessionFor(sessionSettings, bucketName)
        Flow[String]
          .mapAsync(writeSettings.parallelism)(
            id => {
              implicit val executor = materializer.system.dispatcher
              session
                .flatMap(_.remove(id, writeSettings))
                .map(_ => CouchbaseDeleteSuccess(id))
                .recover {
                  case exception => CouchbaseDeleteFailure(id, exception)
                }
            }
          )
      }
      .mapMaterializedValue(_ => NotUsed)
}
