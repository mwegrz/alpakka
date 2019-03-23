/*
 * Copyright (C) 2016-2019 Lightbend Inc. <http://www.lightbend.com>
 */

package akka.stream.alpakka.amqp

import akka.util.ByteString
import com.rabbitmq.client.AMQP.BasicProperties
import com.rabbitmq.client.Envelope

final class ReadResult private (
    val bytes: ByteString,
    val envelope: Envelope,
    val properties: BasicProperties
) {
  override def toString: String =
    s"ReadResult(bytes=$bytes, envelope=$envelope, properties=$properties)"
}

object ReadResult {
  def apply(bytes: ByteString, envelope: Envelope, properties: BasicProperties): ReadResult =
    new ReadResult(bytes, envelope, properties)

  /**
   * Java API
   */
  def create(bytes: ByteString, envelope: Envelope, properties: BasicProperties): ReadResult =
    ReadResult(bytes, envelope, properties)
}

final class WriteMessage private (val bytes: ByteString,
                                  val immediate: Boolean,
                                  val mandatory: Boolean,
                                  val properties: Option[BasicProperties] = None,
                                  val routingKey: Option[String] = None) {

  def withImmediate(value: Boolean): WriteMessage =
    if (value == immediate) this
    else copy(immediate = value)

  def withMandatory(value: Boolean): WriteMessage =
    if (value == mandatory) this
    else copy(mandatory = value)

  def withProperties(properties: BasicProperties): WriteMessage =
    copy(properties = Some(properties))

  def withRoutingKey(routingKey: String): WriteMessage =
    copy(routingKey = Some(routingKey))

  private def copy(immediate: Boolean = immediate,
                   mandatory: Boolean = mandatory,
                   properties: Option[BasicProperties] = properties,
                   routingKey: Option[String] = routingKey) =
    new WriteMessage(bytes, immediate, mandatory, properties, routingKey)

  override def toString: String =
    "WriteMessage(" +
    s"bytes=$bytes, " +
    s"immediate=$immediate, " +
    s"mandatory=$mandatory, " +
    s"properties=$properties, " +
    s"routingKey=$routingKey" +
    ")"
}

object WriteMessage {
  def apply(bytes: ByteString): WriteMessage =
    new WriteMessage(bytes, immediate = false, mandatory = false)

  def apply(bytes: ByteString, immediate: Boolean, mandatory: Boolean): WriteMessage =
    new WriteMessage(bytes, immediate, mandatory)

  /**
   * Java API
   */
  def create(bytes: ByteString): WriteMessage = WriteMessage(bytes)

  /**
   * Java API
   */
  def create(bytes: ByteString, immediate: Boolean, mandatory: Boolean): WriteMessage =
    WriteMessage(bytes, immediate, mandatory)
}
