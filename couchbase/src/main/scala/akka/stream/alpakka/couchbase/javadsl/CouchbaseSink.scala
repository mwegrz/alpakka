/*
 * Copyright (C) 2016-2019 Lightbend Inc. <http://www.lightbend.com>
 */

package akka.stream.alpakka.couchbase.javadsl

import java.util.concurrent.CompletionStage

import akka.stream.alpakka.couchbase._
import akka.stream.javadsl.{Keep, Sink}
import akka.{Done, NotUsed}
import com.couchbase.client.java.document.{Document, JsonDocument}

/**
 * Java API: Factory methods for Couchbase sinks.
 */
object CouchbaseSink {

  /**
   * Create a sink to update or insert a Couchbase [[com.couchbase.client.java.document.JsonDocument JsonDocument]].
   */
  def upsert(sessionSettings: CouchbaseSessionSettings,
             writeSettings: CouchbaseWriteSettings,
             bucketName: String): Sink[JsonDocument, CompletionStage[Done]] =
    CouchbaseFlow
      .upsert(sessionSettings, writeSettings, bucketName)
      .toMat(Sink.ignore(), Keep.right[NotUsed, CompletionStage[Done]])

  /**
   * Create a sink to update or insert a Couchbase document of the given class.
   */
  def upsertDoc[T <: Document[_]](sessionSettings: CouchbaseSessionSettings,
                                  writeSettings: CouchbaseWriteSettings,
                                  bucketName: String): Sink[T, CompletionStage[Done]] =
    CouchbaseFlow
      .upsertDoc[T](sessionSettings, writeSettings, bucketName)
      .toMat(Sink.ignore(), Keep.right[NotUsed, CompletionStage[Done]])

  /**
   * Create a sink to delete documents from Couchbase by `id`.
   */
  def delete(sessionSettings: CouchbaseSessionSettings,
             writeSettings: CouchbaseWriteSettings,
             bucketName: String): Sink[String, CompletionStage[Done]] =
    CouchbaseFlow
      .delete(sessionSettings, writeSettings, bucketName)
      .toMat(Sink.ignore(), Keep.right[NotUsed, CompletionStage[Done]])

}
