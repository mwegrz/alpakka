/*
 * Copyright (C) 2016-2019 Lightbend Inc. <http://www.lightbend.com>
 */

package akka.stream.alpakka.mqtt.streaming
package impl

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Keep, Source}
import akka.stream.testkit.javadsl.TestSink
import akka.stream.testkit.scaladsl.TestSource
import akka.stream.{ActorMaterializer, Materializer}
import akka.testkit.TestKit
import akka.util.ByteString
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

class MqttFrameStageSpec
    extends TestKit(ActorSystem("MqttFrameStageSpec"))
    with WordSpecLike
    with Matchers
    with BeforeAndAfterAll {

  implicit val mat: Materializer = ActorMaterializer()

  val MaxPacketSize = 100

  "framing" should {
    "frame a packet with just a fixed header" in {
      val bytes = ByteString.newBuilder.putByte(0).putByte(0).result()
      Source
        .single(bytes)
        .via(new MqttFrameStage(MaxPacketSize))
        .runWith(TestSink.probe(system))
        .request(1)
        .expectNext(bytes)
        .expectComplete()
    }

    "frame a packet with a fixed and variable header" in {
      val bytes = ByteString.newBuilder.putByte(0).putByte(1).putByte(0).result()
      Source
        .single(bytes)
        .via(new MqttFrameStage(MaxPacketSize))
        .runWith(TestSink.probe(system))
        .request(1)
        .expectNext(bytes)
        .expectComplete()
    }

    "frame two packets from bytes" in {
      val bytes = ByteString.newBuilder.putByte(0).putByte(1).putByte(0).result()
      Source
        .single(bytes ++ bytes)
        .via(new MqttFrameStage(MaxPacketSize))
        .runWith(TestSink.probe(system))
        .request(2)
        .expectNext(bytes, bytes)
        .expectComplete()
    }

    "frame a packet where its length bytes are split" in {
      val bytes0 = ByteString.newBuilder.putByte(0).putByte(0x80.toByte).result()
      val bytes1 = ByteString.newBuilder.putByte(1).putBytes(Array.ofDim(0x80)).result()

      val (pub, sub) =
        TestSource
          .probe(system)
          .via(new MqttFrameStage(MaxPacketSize * 2))
          .toMat(TestSink.probe(system))(Keep.both)
          .run()

      pub.sendNext(bytes0)

      sub.request(1)

      pub.sendNext(bytes1).sendComplete()

      sub
        .expectNext(bytes0 ++ bytes1)
        .expectComplete()
    }

    "fail if packet size exceeds max" in {
      val bytes = ByteString.newBuilder.putByte(0).putByte(MaxPacketSize.toByte).putByte(0).result()
      val ex =
        Source
          .single(bytes)
          .via(new MqttFrameStage(MaxPacketSize))
          .runWith(TestSink.probe(system))
          .request(1)
          .expectError()
      ex.getMessage shouldBe s"Max packet size of $MaxPacketSize exceeded with ${MaxPacketSize + 2}"
    }
  }

  override def afterAll: Unit =
    TestKit.shutdownActorSystem(system)
}
