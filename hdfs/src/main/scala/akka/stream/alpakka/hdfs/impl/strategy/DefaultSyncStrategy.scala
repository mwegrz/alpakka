/*
 * Copyright (C) 2016-2019 Lightbend Inc. <http://www.lightbend.com>
 */

package akka.stream.alpakka.hdfs.impl.strategy

import akka.annotation.InternalApi
import akka.stream.alpakka.hdfs.SyncStrategy

/**
 * Internal API
 */
@InternalApi
private[hdfs] object DefaultSyncStrategy {
  final case class CountSyncStrategy(
      executeCount: Long = 0,
      count: Long
  ) extends SyncStrategy {
    def should(): Boolean = executeCount >= count
    def reset(): SyncStrategy = copy(executeCount = 0)
    def update(offset: Long): SyncStrategy = copy(executeCount = executeCount + 1)
  }

  case object NoSyncStrategy extends SyncStrategy {
    def should(): Boolean = false
    def reset(): SyncStrategy = this
    def update(offset: Long): SyncStrategy = this
  }
}
