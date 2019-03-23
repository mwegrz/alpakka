/*
 * Copyright (C) 2016-2019 Lightbend Inc. <http://www.lightbend.com>
 */

package docs.scaladsl

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.alpakka.kinesisfirehose.KinesisFirehoseFlowSettings
import akka.stream.alpakka.kinesisfirehose.scaladsl.{KinesisFirehoseFlow, KinesisFirehoseSink}
import akka.stream.scaladsl.{Flow, Sink}
import akka.stream.{ActorMaterializer, Materializer}
import com.amazonaws.services.kinesisfirehose.AmazonKinesisFirehoseAsyncClientBuilder
import com.amazonaws.services.kinesisfirehose.model.{PutRecordBatchResponseEntry, Record}

import scala.concurrent.duration._

object KinesisFirehoseSnippets {

  //#init-client
  implicit val system: ActorSystem = ActorSystem()
  implicit val materializer: Materializer = ActorMaterializer()

  implicit val amazonKinesisFirehoseAsync: com.amazonaws.services.kinesisfirehose.AmazonKinesisFirehoseAsync =
    AmazonKinesisFirehoseAsyncClientBuilder.defaultClient()

  system.registerOnTermination(amazonKinesisFirehoseAsync.shutdown())
  //#init-client

  //#flow-settings
  val flowSettings = KinesisFirehoseFlowSettings
    .create()
    .withParallelism(1)
    .withMaxBatchSize(500)
    .withMaxRecordsPerSecond(5000)
    .withMaxBytesPerSecond(4000000)
    .withMaxRetries(5)
    .withBackoffStrategy(KinesisFirehoseFlowSettings.Exponential)
    .withRetryInitialTimeout(100.millis)

  val defaultFlowSettings = KinesisFirehoseFlowSettings.Defaults
  //#flow-settings

  //#flow-sink
  val flow1: Flow[Record, PutRecordBatchResponseEntry, NotUsed] = KinesisFirehoseFlow("myStreamName")

  val flow2: Flow[Record, PutRecordBatchResponseEntry, NotUsed] = KinesisFirehoseFlow("myStreamName", flowSettings)

  val sink1: Sink[Record, NotUsed] = KinesisFirehoseSink("myStreamName")
  val sink2: Sink[Record, NotUsed] = KinesisFirehoseSink("myStreamName", flowSettings)
  //#flow-sink

}
