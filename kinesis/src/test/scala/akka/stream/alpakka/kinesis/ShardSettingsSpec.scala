/*
 * Copyright (C) 2016-2019 Lightbend Inc. <http://www.lightbend.com>
 */

package akka.stream.alpakka.kinesis

import java.time.Instant

import com.amazonaws.services.kinesis.model.ShardIteratorType
import org.scalatest.{Matchers, WordSpec}

class ShardSettingsSpec extends WordSpec with Matchers {
  val baseSettings = ShardSettings("name", "shardid")
  "ShardSettings" must {
    "require a timestamp for shard iterator type is AT_TIMESTAMP" in {
      a[IllegalArgumentException] should be thrownBy baseSettings
        .withShardIteratorType(ShardIteratorType.AT_TIMESTAMP)
    }
    "accept a valid timestamp for shard iterator type AT_TIMESTAMP" in {
      noException should be thrownBy baseSettings
        .withAtTimestamp(Instant.now())
        .withShardIteratorType(ShardIteratorType.AT_TIMESTAMP)
    }
    "require a sequence number for iterator type AT_SEQUENCE_NUMBER" in {
      a[IllegalArgumentException] should be thrownBy baseSettings
        .withShardIteratorType(ShardIteratorType.AT_SEQUENCE_NUMBER)
    }
    "accept a valid sequence number for iterator type AT_SEQUENCE_NUMBER" in {
      noException should be thrownBy baseSettings
        .withStartingSequenceNumber("SQC")
        .withShardIteratorType(ShardIteratorType.AT_SEQUENCE_NUMBER)
    }
    "require a sequence number for iterator type AFTER_SEQUENCE_NUMBER" in {
      a[IllegalArgumentException] should be thrownBy baseSettings
        .withStartingSequenceNumber(null)
        .withShardIteratorType(ShardIteratorType.AFTER_SEQUENCE_NUMBER)
    }
    "accept a valid sequence number for iterator type AFTER_SEQUENCE_NUMBER" in {
      noException should be thrownBy baseSettings
        .withStartingSequenceNumber("SQC")
        .withShardIteratorType(ShardIteratorType.AFTER_SEQUENCE_NUMBER)
    }
    "require a valid limit" in {
      a[IllegalArgumentException] should be thrownBy baseSettings.withLimit(10001)
      a[IllegalArgumentException] should be thrownBy baseSettings.withLimit(-1)
    }
    "accept a valid limit" in {
      noException should be thrownBy baseSettings.withLimit(500)
    }
  }
}
