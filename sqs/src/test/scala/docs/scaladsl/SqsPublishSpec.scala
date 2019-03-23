/*
 * Copyright (C) 2016-2019 Lightbend Inc. <http://www.lightbend.com>
 */

package docs.scaladsl

import akka.Done
import akka.stream.alpakka.sqs._
import akka.stream.alpakka.sqs.scaladsl._
import akka.stream.scaladsl.{Sink, Source}
import org.scalatest.{FlatSpec, Matchers}
import software.amazon.awssdk.services.sqs.model.{Message, ReceiveMessageRequest, SendMessageRequest}

import scala.collection.JavaConverters._
import scala.concurrent.duration._

class SqsPublishSpec extends FlatSpec with Matchers with DefaultTestContext {

  abstract class IntegrationFixture(fifo: Boolean = false) {
    val queueUrl: String = if (fifo) randomFifoQueueUrl() else randomQueueUrl()
    implicit val awsSqsClient = sqsClient

    def receiveMessage(): Message =
      awsSqsClient
        .receiveMessage(ReceiveMessageRequest.builder().queueUrl(queueUrl).build())
        .get()
        .messages()
        .asScala
        .head

    def receiveMessages(maxNumberOfMessages: Int): Seq[Message] = {
      // see https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/sqs/model/ReceiveMessageRequest.html
      require(maxNumberOfMessages > 0 && maxNumberOfMessages <= 10, "maxNumberOfMessages must be in 1 to 10")

      val request =
        ReceiveMessageRequest
          .builder()
          .queueUrl(queueUrl)
          .maxNumberOfMessages(maxNumberOfMessages)
          .build()

      awsSqsClient.receiveMessage(request).get().messages().asScala
    }
  }

  "SqsPublishSettings" should "construct settings" in {
    //#SqsPublishSettings
    val sinkSettings =
      SqsPublishSettings()
        .withMaxInFlight(10)
    //#SqsPublishSettings
    sinkSettings.maxInFlight shouldBe 10
  }

  it should "require valid maxInFlight" in {
    a[IllegalArgumentException] should be thrownBy {
      SqsPublishSettings().withMaxInFlight(0)
    }
  }

  it should "accept valid parameters" in {
    SqsPublishSettings().withMaxInFlight(1)
  }

  "SqsPublishBatchSettings" should "construct settings" in {
    //#SqsPublishBatchSettings
    val batchSettings =
      SqsPublishBatchSettings()
        .withConcurrentRequests(1)
    //#SqsPublishBatchSettings
    batchSettings.concurrentRequests shouldBe 1
  }

  "SqsPublishGroupedSettings" should "construct settings" in {
    //#SqsPublishGroupedSettings
    val batchSettings =
      SqsPublishGroupedSettings()
        .withMaxBatchSize(10)
        .withMaxBatchWait(500.millis)
        .withConcurrentRequests(1)
    //#SqsPublishGroupedSettings
    batchSettings.concurrentRequests shouldBe 1
  }

  "PublishSink" should "publish and pull a message" taggedAs Integration in new IntegrationFixture {
    val future =
      //#run-string
      Source
        .single("alpakka")
        .runWith(SqsPublishSink(queueUrl))
    //#run-string
    future.futureValue shouldBe Done

    receiveMessage().body() shouldBe "alpakka"
  }

  it should "publish and pull a message provided as a SendMessageRequest" taggedAs Integration in new IntegrationFixture {
    val future =
      //#run-send-request
      // for fix SQS queue
      Source
        .single(SendMessageRequest.builder().messageBody("alpakka").build())
        .runWith(SqsPublishSink.messageSink(queueUrl))

    //#run-send-request

    future.futureValue shouldBe Done

    receiveMessage().body() shouldBe "alpakka"
  }

  it should "publish and pull a message provided as a SendMessageRequest with dynamic queue" taggedAs Integration in new IntegrationFixture {
    val future =
      //#run-send-request
      // for dynamic SQS queues
      Source
        .single(SendMessageRequest.builder().messageBody("alpakka").queueUrl(queueUrl).build())
        .runWith(SqsPublishSink.messageSink())
    //#run-send-request

    future.futureValue shouldBe Done

    receiveMessage().body() shouldBe "alpakka"
  }

  it should "publish messages by grouping and pull them" taggedAs Integration in new IntegrationFixture {
    //#group
    val messages = for (i <- 0 until 10) yield s"Message - $i"

    val future = Source(messages)
      .runWith(SqsPublishSink.grouped(queueUrl, SqsPublishGroupedSettings.Defaults.withMaxBatchSize(2)))
    //#group

    future.futureValue shouldBe Done

    receiveMessages(10) should have size 10
  }

  it should "publish batch of messages and pull them" taggedAs Integration in new IntegrationFixture {
    //#batch-string
    val messages = for (i <- 0 until 10) yield s"Message - $i"

    val future = Source
      .single(messages)
      .runWith(SqsPublishSink.batch(queueUrl))
    //#batch-string

    future.futureValue shouldBe Done

    receiveMessages(10) should have size 10
  }

  it should "publish batch of SendMessageRequests and pull them" taggedAs Integration in new IntegrationFixture {
    //#batch-send-request
    val messages = for (i <- 0 until 10) yield SendMessageRequest.builder().messageBody(s"Message - $i").build()

    val future = Source
      .single(messages)
      .runWith(SqsPublishSink.batchedMessageSink(queueUrl))
    //#batch-send-request

    future.futureValue shouldBe Done

    receiveMessages(10) should have size 10
  }

  "PublishFlow" should "put message in a flow, then pass the result further" taggedAs Integration in new IntegrationFixture {
    val future =
      //#flow
      // for fix SQS queue
      Source
        .single(SendMessageRequest.builder().messageBody("alpakka").build())
        .via(SqsPublishFlow(queueUrl))
        .runWith(Sink.head)

    //#flow

    val result = future.futureValue
    result.metadata.md5OfMessageBody() shouldBe md5HashString("alpakka")
    result.fifoMessageIdentifiers shouldBe empty

    receiveMessage().body() shouldBe "alpakka"
  }

  it should "put message in a flow, then pass the result further with dynamic queue" taggedAs Integration in new IntegrationFixture {
    val future =
      //#flow
      // for dynamic SQS queues
      Source
        .single(SendMessageRequest.builder().messageBody("alpakka").queueUrl(queueUrl).build())
        .via(SqsPublishFlow())
        .runWith(Sink.head)
    //#flow

    val result = future.futureValue
    result.metadata.md5OfMessageBody() shouldBe md5HashString("alpakka")
    result.fifoMessageIdentifiers shouldBe empty

    receiveMessage().body() shouldBe "alpakka"
  }

  ignore should "put message in a flow, then pass the result further with fifo queues" taggedAs Integration in new IntegrationFixture(
    fifo = true
  ) {
    // elasticmq does not provide proper fifo support (see https://github.com/adamw/elasticmq/issues/92)
    // set your fifo sqs queue url and awsSqsClient manually
    // override val queue = "https://sqs.us-east-1.amazonaws.com/$AWS_ACCOUNT_ID/$queue_name.fifo"
    // override implicit val awsSqsClient: AmazonSQSAsync = AmazonSQSAsyncClientBuilder.standard().build()

    val future =
      Source
        .single(
          SendMessageRequest
            .builder()
            .messageBody("alpakka")
            .messageGroupId("group-id")
            .messageDeduplicationId(s"deduplication-id")
            .build()
        )
        .via(SqsPublishFlow(queueUrl))
        .runWith(Sink.head)

    val result = future.futureValue
    result.metadata.md5OfMessageBody() shouldBe md5HashString("alpakka")
    result.fifoMessageIdentifiers.map(_.sequenceNumber) shouldBe defined
    result.fifoMessageIdentifiers.map(_.messageGroupId) shouldBe Some("group-id")
    result.fifoMessageIdentifiers.flatMap(_.messageDeduplicationId) shouldBe Some("deduplication-id")

    receiveMessage().body() shouldBe "alpakka"
  }

  def md5HashString(s: String): String = {
    import java.security.MessageDigest
    import java.math.BigInteger
    val md = MessageDigest.getInstance("MD5")
    val digest = md.digest(s.getBytes)
    val bigInt = new BigInteger(1, digest)
    val hashedString = bigInt.toString(16)
    hashedString
  }
}
