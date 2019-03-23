/*
 * Copyright (C) 2016-2019 Lightbend Inc. <http://www.lightbend.com>
 */

package akka.stream.alpakka.sqs

final class SqsPublishSettings private (val maxInFlight: Int) {
  require(maxInFlight > 0)

  def withMaxInFlight(maxInFlight: Int): SqsPublishSettings = copy(maxInFlight = maxInFlight)

  private def copy(maxInFlight: Int = maxInFlight) = new SqsPublishSettings(maxInFlight)

  override def toString: String =
    "SqsPublishSettings(" +
    s"maxInFlight=$maxInFlight" +
    ")"
}

object SqsPublishSettings {
  val Defaults = new SqsPublishSettings(maxInFlight = 10)

  /**
   * Scala API
   */
  def apply(): SqsPublishSettings = Defaults

  /**
   * Java API
   */
  def create(): SqsPublishSettings = Defaults
}
