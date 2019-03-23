/*
 * Copyright (C) 2016-2019 Lightbend Inc. <http://www.lightbend.com>
 */

package akka.stream.alpakka.sqs.scaladsl

import java.net.URI

import akka.actor.ActorSystem
import akka.stream.alpakka.sqs.SqsSourceSettings
import akka.stream.{ActorMaterializer, Materializer}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time._
import org.scalatest.{BeforeAndAfterAll, Suite, Tag}
import software.amazon.awssdk.auth.credentials.{AwsBasicCredentials, StaticCredentialsProvider}
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest

import scala.collection.JavaConverters._
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Random

trait DefaultTestContext extends BeforeAndAfterAll with ScalaFutures { this: Suite =>

  //#init-mat
  implicit val system: ActorSystem = ActorSystem()
  implicit val mat: Materializer = ActorMaterializer()
  //#init-mat

  // endpoint of the elasticmq docker container
  val sqsEndpoint: String = "http://localhost:9324"

  object Integration extends Tag("akka.stream.alpakka.sqs.scaladsl.Integration")

  implicit val pc = PatienceConfig(scaled(Span(10, Seconds)), scaled(Span(20, Millis)))

  lazy val sqsClient = createAsyncClient(sqsEndpoint)

  //ElasticMQ has a bug: when you set wait time seconds > 0,
  //sometimes the server does not return any message and blocks the 20 seconds, even if a message arrives later.
  //this helps the tests to become a little less intermittent. =)
  val sqsSourceSettings = SqsSourceSettings.Defaults.withWaitTimeSeconds(0)

  def randomQueueUrl(): String =
    sqsClient
      .createQueue(CreateQueueRequest.builder().queueName(s"queue-${Random.nextInt}").build())
      .get()
      .queueUrl()

  val fifoQueueRequest =
    CreateQueueRequest
      .builder()
      .queueName(s"queue-${Random.nextInt}.fifo")
      .attributesWithStrings(Map("FifoQueue" -> "true", "ContentBasedDeduplication" -> "true").asJava)
      .build()

  def randomFifoQueueUrl(): String = sqsClient.createQueue(fifoQueueRequest).get().queueUrl()

  override protected def afterAll(): Unit =
    try {
      Await.ready(system.terminate(), 5.seconds)
    } finally {
      super.afterAll()
    }

  def createAsyncClient(sqsEndpoint: String): SqsAsyncClient = {
    //#init-client
    implicit val awsSqsClient = SqsAsyncClient
      .builder()
      .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("x", "x")))
      .endpointOverride(URI.create(sqsEndpoint))
      .region(Region.EU_CENTRAL_1)
      .build()

    system.registerOnTermination(awsSqsClient.close())
    //#init-client
    awsSqsClient
  }
}
