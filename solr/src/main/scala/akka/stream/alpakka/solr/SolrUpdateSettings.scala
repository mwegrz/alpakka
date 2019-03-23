/*
 * Copyright (C) 2016-2019 Lightbend Inc. <http://www.lightbend.com>
 */

package akka.stream.alpakka.solr

final class SolrUpdateSettings private (
    val commitWithin: Int
) {

  /**
   * Set max time (in ms) before a commit will happen
   */
  def withCommitWithin(value: Int): SolrUpdateSettings = copy(commitWithin = value)

  private def copy(
      commitWithin: Int = commitWithin
  ): SolrUpdateSettings = new SolrUpdateSettings(
    commitWithin = commitWithin
  )

  override def toString =
    "SolrUpdateSettings(" +
    s"commitWithin=$commitWithin" +
    ")"
}

object SolrUpdateSettings {

  val Defaults = new SolrUpdateSettings(-1)

  /** Scala API */
  def apply(): SolrUpdateSettings = Defaults

  /** Java API */
  def create(): SolrUpdateSettings = Defaults

}
