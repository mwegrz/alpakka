/*
 * Copyright (C) 2016-2019 Lightbend Inc. <http://www.lightbend.com>
 */

package akka.stream.alpakka.couchbase

import java.util.concurrent.CompletionStage
import java.util.concurrent.atomic.AtomicReference

import akka.actor.{ActorSystem, ExtendedActorSystem, Extension, ExtensionId, ExtensionIdProvider}
import akka.event.Logging
import akka.stream.alpakka.couchbase.javadsl.{CouchbaseSession => JCouchbaseSession}
import akka.stream.alpakka.couchbase.scaladsl.CouchbaseSession

import scala.annotation.tailrec
import scala.compat.java8.FutureConverters._
import scala.concurrent.{Future, Promise}

/**
 * This Couchbase session registry makes it possible to share Couchbase sessions between multiple use sites
 * in the same `ActorSystem` (important for the Couchbase Akka Persistence plugin where it is shared between journal,
 * query plugin and snapshot plugin)
 */
object CouchbaseSessionRegistry extends ExtensionId[CouchbaseSessionRegistry] with ExtensionIdProvider {
  def createExtension(system: ExtendedActorSystem): CouchbaseSessionRegistry =
    new CouchbaseSessionRegistry(system)

  /**
   * Java API: get the session registry
   */
  override def get(system: ActorSystem): CouchbaseSessionRegistry =
    super.get(system)

  override def lookup(): ExtensionId[CouchbaseSessionRegistry] = this

  private case class SessionKey(settings: CouchbaseSessionSettings, bucketName: String)
}

final class CouchbaseSessionRegistry(system: ExtendedActorSystem) extends Extension {

  import CouchbaseSessionRegistry._

  private val log = Logging(system, classOf[CouchbaseSessionRegistry])

  private val sessions = new AtomicReference(Map.empty[SessionKey, Future[CouchbaseSession]])

  /**
   * Scala API: Get an existing session or start a new one with the given settings and bucket name,
   * makes it possible to share one session across plugins.
   *
   * Note that the session must not be stopped manually, it is shut down when the actor system is shutdown,
   * if you need a more fine grained life cycle control, create the CouchbaseSession manually instead.
   */
  def sessionFor(settings: CouchbaseSessionSettings, bucketName: String): Future[CouchbaseSession] = {
    val key = SessionKey(settings, bucketName)
    sessions.get.get(key) match {
      case Some(futureSession) => futureSession
      case _ => startSession(key)
    }
  }

  /**
   * Java API: Get an existing session or start a new one with the given settings and bucket name,
   * makes it possible to share one session across plugins.
   *
   * Note that the session must not be stopped manually, it is shut down when the actor system is shutdown,
   * if you need a more fine grained life cycle control, create the CouchbaseSession manually instead.
   */
  def getSessionFor(settings: CouchbaseSessionSettings, bucketName: String): CompletionStage[JCouchbaseSession] =
    sessionFor(settings, bucketName).map(_.asJava)(system.dispatcher).toJava

  @tailrec
  private def startSession(key: SessionKey): Future[CouchbaseSession] = {
    val promise = Promise[CouchbaseSession]()
    val oldSessions = sessions.get()
    val newSessions = oldSessions.updated(key, promise.future)
    if (sessions.compareAndSet(oldSessions, newSessions)) {
      // we won cas, initialize session
      def nodesAsString = key.settings.nodes.mkString("\"", "\", \"", "\"")
      log.info("Starting Couchbase session for nodes [{}]", nodesAsString)
      promise.completeWith(CouchbaseSession(key.settings, key.bucketName)(system.dispatcher))
      val future = promise.future
      system.registerOnTermination {
        future.foreach { session =>
          session.close()
          log.info("Shutting down couchbase session for nodes [{}]", nodesAsString)
        }(system.dispatcher)
      }
      future
    } else {
      // we lost cas (could be concurrent call for some other key though), retry
      startSession(key)
    }
  }

}
