/*
 * Copyright (C) 2016-2019 Lightbend Inc. <http://www.lightbend.com>
 */

package docs.scaladsl

import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.stream.alpakka.kudu.{KuduAttributes, KuduTableSettings}
import akka.stream.alpakka.kudu.scaladsl.KuduTable
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.{ActorMaterializer, Materializer}
import akka.testkit.TestKit
import org.apache.kudu.client.{CreateTableOptions, KuduClient, PartialRow}
import org.apache.kudu.{ColumnSchema, Schema, Type}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.collection.JavaConverters._
import scala.concurrent.Future
import scala.concurrent.duration._

class KuduTableSpec
    extends TestKit(ActorSystem())
    with WordSpecLike
    with Matchers
    with BeforeAndAfterAll
    with ScalaFutures {

  implicit val materializer: Materializer = ActorMaterializer()
  implicit val defaultPatience: PatienceConfig =
    PatienceConfig(timeout = 5.seconds, interval = 100.millis)

  override def afterAll(): Unit = TestKit.shutdownActorSystem(system)

  //#configure
  // Kudu Schema
  val cols = List(new ColumnSchema.ColumnSchemaBuilder("key", Type.INT32).key(true).build,
                  new ColumnSchema.ColumnSchemaBuilder("value", Type.STRING).build)
  val schema = new Schema(cols.asJava)

  // Converter function
  case class Person(id: Int, name: String)
  val kuduConverter: Person => PartialRow = { person =>
    val partialRow = schema.newPartialRow()
    partialRow.addInt(0, person.id)
    partialRow.addString(1, person.name)
    partialRow
  }

  // Kudu table options
  val rangeKeys = List("key")
  val createTableOptions = new CreateTableOptions().setNumReplicas(1).setRangePartitionColumns(rangeKeys.asJava)

  // Alpakka settings
  val kuduTableSettings = KuduTableSettings("test", schema, createTableOptions, kuduConverter)
  //#configure

  "Kudu stages " must {

    "sinks in kudu" in {
      //#sink
      val sink: Sink[Person, Future[Done]] =
        KuduTable.sink(kuduTableSettings.withTableName("Sink"))

      val f = Source(1 to 10)
        .map(i => Person(i, s"zozo_$i"))
        .runWith(sink)
      //#sink

      f.futureValue should be(Done)
    }

    "flows through kudu" in {
      //#flow
      val flow: Flow[Person, Person, NotUsed] =
        KuduTable.flow(kuduTableSettings.withTableName("Flow"))

      val f = Source(11 to 20)
        .map(i => Person(i, s"zozo_$i"))
        .via(flow)
        .runWith(Sink.fold(0)((a, d) => a + d.id))
      //#flow

      f.futureValue should be(155)
    }

    "custom client" in {
      // #attributes
      val masterAddress = "localhost:7051"
      val client = new KuduClient.KuduClientBuilder(masterAddress).build
      system.registerOnTermination(client.shutdown())

      val flow: Flow[Person, Person, NotUsed] =
        KuduTable
          .flow(kuduTableSettings.withTableName("Flow"))
          .withAttributes(KuduAttributes.client(client))
      // #attributes

      val f = Source(11 to 20)
        .map(i => Person(i, s"zozo_$i"))
        .via(flow)
        .runWith(Sink.fold(0)((a, d) => a + d.id))

      f.futureValue should be(155)
    }

  }

}
