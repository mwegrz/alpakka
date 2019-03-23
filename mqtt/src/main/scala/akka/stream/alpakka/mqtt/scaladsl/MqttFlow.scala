/*
 * Copyright (C) 2016-2019 Lightbend Inc. <http://www.lightbend.com>
 */

package akka.stream.alpakka.mqtt.scaladsl

import akka.Done
import akka.stream.alpakka.mqtt._
import akka.stream.alpakka.mqtt.impl.MqttFlowStage
import akka.stream.scaladsl.Flow

import scala.concurrent.Future

/**
 * Scala API
 *
 * MQTT flow factory.
 */
object MqttFlow {

  /**
   * Create a flow to send messages to MQTT AND subscribe to MQTT messages (without a commit handle).
   *
   * The materialized value completes on successful connection to the MQTT broker.
   *
   * @param bufferSize max number of messages read from MQTT before back-pressure applies
   * @param defaultQos Quality of service level applied for messages not specifying a message specific value
   */
  @deprecated("use atMostOnce instead", "1.0-M1")
  def apply(sourceSettings: MqttSourceSettings,
            bufferSize: Int,
            defaultQos: MqttQoS): Flow[MqttMessage, MqttMessage, Future[Done]] =
    atMostOnce(sourceSettings.connectionSettings,
               MqttSubscriptions(sourceSettings.subscriptions),
               bufferSize,
               defaultQos)

  /**
   * Create a flow to send messages to MQTT AND subscribe to MQTT messages (without a commit handle).
   *
   * The materialized value completes on successful connection to the MQTT broker.
   *
   * @param bufferSize max number of messages read from MQTT before back-pressure applies
   * @param defaultQos Quality of service level applied for messages not specifying a message specific value
   */
  @deprecated("use atMostOnce with MqttConnectionSettings and MqttSubscriptions instead", "1.0-M1")
  def atMostOnce(sourceSettings: MqttSourceSettings,
                 bufferSize: Int,
                 defaultQos: MqttQoS): Flow[MqttMessage, MqttMessage, Future[Done]] =
    Flow
      .fromGraph(
        new MqttFlowStage(sourceSettings.connectionSettings, sourceSettings.subscriptions, bufferSize, defaultQos)
      )
      .map(_.message)

  /**
   * Create a flow to send messages to MQTT AND subscribe to MQTT messages (without a commit handle).
   *
   * The materialized value completes on successful connection to the MQTT broker.
   *
   * @param bufferSize max number of messages read from MQTT before back-pressure applies
   * @param defaultQos Quality of service level applied for messages not specifying a message specific value
   */
  def atMostOnce(connectionSettings: MqttConnectionSettings,
                 subscriptions: MqttSubscriptions,
                 bufferSize: Int,
                 defaultQos: MqttQoS): Flow[MqttMessage, MqttMessage, Future[Done]] =
    Flow
      .fromGraph(
        new MqttFlowStage(connectionSettings, subscriptions.subscriptions, bufferSize, defaultQos)
      )
      .map(_.message)

  /**
   * Create a flow to send messages to MQTT AND subscribe to MQTT messages with a commit handle to acknowledge message reception.
   *
   * The materialized value completes on successful connection to the MQTT broker.
   *
   * @param bufferSize max number of messages read from MQTT before back-pressure applies
   * @param defaultQos Quality of service level applied for messages not specifying a message specific value
   */
  @deprecated("use atMostOnce with MqttConnectionSettings and MqttSubscriptions instead", "1.0-M1")
  def atLeastOnce(sourceSettings: MqttSourceSettings,
                  bufferSize: Int,
                  defaultQos: MqttQoS): Flow[MqttMessage, MqttMessageWithAck, Future[Done]] =
    Flow.fromGraph(
      new MqttFlowStage(sourceSettings.connectionSettings,
                        sourceSettings.subscriptions,
                        bufferSize,
                        defaultQos,
                        manualAcks = true)
    )

  /**
   * Create a flow to send messages to MQTT AND subscribe to MQTT messages with a commit handle to acknowledge message reception.
   *
   * The materialized value completes on successful connection to the MQTT broker.
   *
   * @param bufferSize max number of messages read from MQTT before back-pressure applies
   * @param defaultQos Quality of service level applied for messages not specifying a message specific value
   */
  def atLeastOnce(connectionSettings: MqttConnectionSettings,
                  subscriptions: MqttSubscriptions,
                  bufferSize: Int,
                  defaultQos: MqttQoS): Flow[MqttMessage, MqttMessageWithAck, Future[Done]] =
    Flow.fromGraph(
      new MqttFlowStage(connectionSettings, subscriptions.subscriptions, bufferSize, defaultQos, manualAcks = true)
    )
}
