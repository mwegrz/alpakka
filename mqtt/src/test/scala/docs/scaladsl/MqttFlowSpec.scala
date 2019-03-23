/*
 * Copyright (C) 2016-2019 Lightbend Inc. <http://www.lightbend.com>
 */

package docs.scaladsl

import akka.Done
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import akka.stream.alpakka.mqtt.scaladsl.MqttFlow
import akka.stream.alpakka.mqtt._
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import akka.testkit.TestKit
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

class MqttFlowSpec
    extends TestKit(ActorSystem("MqttFlowSpec"))
    with WordSpecLike
    with Matchers
    with BeforeAndAfterAll
    with ScalaFutures {

  val timeout = 5.seconds
  implicit val defaultPatience =
    PatienceConfig(timeout = 5.seconds, interval = 100.millis)

  implicit val mat: Materializer = ActorMaterializer()
  val connectionSettings = MqttConnectionSettings(
    "tcp://localhost:1883",
    "test-client",
    new MemoryPersistence
  )

  override def afterAll() = TestKit.shutdownActorSystem(system)

  "mqtt flow" should {
    "establish a bidirectional connection and subscribe to a topic" in {
      val topic = "flow-spec/topic"
      //#create-flow
      val mqttFlow: Flow[MqttMessage, MqttMessage, Future[Done]] =
        MqttFlow.atMostOnce(
          connectionSettings.withClientId("flow-spec/flow"),
          MqttSubscriptions(topic, MqttQoS.AtLeastOnce),
          bufferSize = 8,
          MqttQoS.AtLeastOnce
        )
      //#create-flow

      val source = Source.maybe[MqttMessage]

      //#run-flow
      val ((mqttMessagePromise, subscribed), result) = source
        .viaMat(mqttFlow)(Keep.both)
        .toMat(Sink.seq)(Keep.both)
        .run()
      //#run-flow

      Await.ready(subscribed, timeout)
      mqttMessagePromise.success(None)
      noException should be thrownBy result.futureValue
    }
  }
}
