/*
 * Copyright (C) 2016-2019 Lightbend Inc. <http://www.lightbend.com>
 */

package akka.stream.alpakka.sqs.scaladsl

import java.util

import akka._
import akka.stream._
import akka.stream.alpakka.sqs.SqsSourceSettings
import akka.stream.impl.fusing.GraphStages.SimpleLinearGraphStage
import akka.stream.scaladsl.Source
import akka.stream.stage.{GraphStageLogic, InHandler, OutHandler}
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.{Message, QueueAttributeName, ReceiveMessageRequest}

import scala.collection.JavaConverters._
import scala.compat.java8.FutureConverters._

/**
 * Scala API to create SQS sources.
 */
object SqsSource {

  /**
   * creates a [[akka.stream.scaladsl.Source Source]] for a SQS queue using [[software.amazon.awssdk.services.sqs.SqsAsyncClient SqsAsyncClient]]
   */
  def apply(
      queueUrl: String,
      settings: SqsSourceSettings = SqsSourceSettings.Defaults
  )(implicit sqsClient: SqsAsyncClient): Source[Message, NotUsed] =
    Source
      .repeat {
        val requestBuilder =
          ReceiveMessageRequest
            .builder()
            .queueUrl(queueUrl)
            .attributeNames(settings.attributeNames.map(_.name).map(QueueAttributeName.fromValue).asJava)
            .messageAttributeNames(settings.messageAttributeNames.map(_.name).asJava)
            .maxNumberOfMessages(settings.maxBatchSize)
            .waitTimeSeconds(settings.waitTimeSeconds)

        settings.visibilityTimeout match {
          case None => requestBuilder.build()
          case Some(t) => requestBuilder.visibilityTimeout(t.toSeconds.toInt).build()
        }
      }
      .mapAsync(settings.maxBufferSize / settings.maxBatchSize)(sqsClient.receiveMessage(_).toScala)
      .map(_.messages().asScala.toList)
      .via(new SimpleLinearGraphStage[List[Message]] {
        val buffer: util.ArrayDeque[List[Message]] = new java.util.ArrayDeque[List[Message]]()

        override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {
          def outHandler: OutHandler = new OutHandler {
            override def onPull(): Unit = if (!hasBeenPulled(in)) pull(in)
          }

          def inHandler: InHandler = new InHandler {
            override def onPush(): Unit = {
              val messages = grab(in)

              if (messages.nonEmpty) buffer.offer(messages)
              if (settings.closeOnEmptyReceive && buffer.isEmpty) completeStage()
              if (!hasBeenPulled(in)) pull(in)
              if (!buffer.isEmpty && isAvailable(out)) push(out, buffer.poll())
            }
          }

          setHandler(in, inHandler)
          setHandler(out, outHandler)
        }
      })
      .mapConcat(identity)
      .buffer(settings.maxBufferSize, OverflowStrategy.backpressure)
}
