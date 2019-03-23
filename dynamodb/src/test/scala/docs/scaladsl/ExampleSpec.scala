/*
 * Copyright (C) 2016-2019 Lightbend Inc. <http://www.lightbend.com>
 */

package docs.scaladsl

import java.lang

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.alpakka.dynamodb.{DynamoAttributes, DynamoClient, DynamoSettings}
import akka.stream.{ActorMaterializer, Materializer}
import akka.stream.alpakka.dynamodb.scaladsl._
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.testkit.scaladsl.StreamTestKit.assertAllStagesStopped
import akka.testkit.TestKit
import com.amazonaws.services.dynamodbv2.model._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.duration._
import scala.concurrent.Future

class ExampleSpec
    extends TestKit(ActorSystem("ExampleSpec"))
    with WordSpecLike
    with Matchers
    with BeforeAndAfterAll
    with ScalaFutures {

  implicit val materializer: Materializer = ActorMaterializer()
  override implicit val patienceConfig: PatienceConfig = PatienceConfig(5.seconds, 100.millis)

  override def beforeAll() = {
    System.setProperty("aws.accessKeyId", "someKeyId")
    System.setProperty("aws.secretKey", "someSecretKey")
  }

  override def afterAll(): Unit = shutdown()

  "DynamoDB" should {

    "provide a simple usage example" in {

      //##simple-request
      val listTablesResult: Future[ListTablesResult] =
        DynamoDb.single(new ListTablesRequest())
      //##simple-request

      listTablesResult.futureValue
    }

    "allow multiple requests - explicit types" in assertAllStagesStopped {
      import akka.stream.alpakka.dynamodb.AwsOp._
      val source = Source
        .single[CreateTable](new CreateTableRequest().withTableName("testTable"))
        .via(DynamoDb.flow)
        .map[DescribeTable](
          result => new DescribeTableRequest().withTableName(result.getTableDescription.getTableName)
        )
        .via(DynamoDb.flow)
        .map(_.getTable.getItemCount)
      val streamCompletion = source.runWith(Sink.seq)
      streamCompletion.failed.futureValue shouldBe a[AmazonDynamoDBException]
    }

    "allow multiple requests" in assertAllStagesStopped {
      //##flow
      import akka.stream.alpakka.dynamodb.AwsOp._
      val source: Source[String, NotUsed] = Source
        .single[CreateTable](new CreateTableRequest().withTableName("testTable"))
        .via(DynamoDb.flow)
        .map(_.getTableDescription.getTableArn)
      //##flow
      val streamCompletion = source.runWith(Sink.seq)
      streamCompletion.failed.futureValue shouldBe a[AmazonDynamoDBException]
    }

    "allow multiple requests - single source" in assertAllStagesStopped {
      import akka.stream.alpakka.dynamodb.AwsOp._
      val source: Source[lang.Long, NotUsed] = DynamoDb
        .source(new CreateTableRequest().withTableName("testTable")) // creating a source from a single req is common enough to warrant a utility function
        .map[DescribeTable](result => new DescribeTableRequest().withTableName(result.getTableDescription.getTableName))
        .via(DynamoDb.flow)
        .map(_.getTable.getItemCount)
      val streamCompletion = source.runWith(Sink.seq)
      streamCompletion.failed.futureValue shouldBe a[AmazonDynamoDBException]
    }

    "provide a paginated requests example" in assertAllStagesStopped {
      //##paginated
      val scanPages: Source[ScanResult, NotUsed] =
        DynamoDb.source(new ScanRequest().withTableName("testTable"))
      //##paginated
      val streamCompletion = scanPages.runWith(Sink.seq)
      streamCompletion.failed.futureValue shouldBe a[AmazonDynamoDBException]
    }

    "use client from attributes" in assertAllStagesStopped {
      // #attributes
      val settings = DynamoSettings(system).withRegion("custom-region")
      val client = DynamoClient(settings)

      val source: Source[ListTablesResult, NotUsed] =
        DynamoDb
          .source(new ListTablesRequest())
          .withAttributes(DynamoAttributes.client(client))
      // #attributes

      source.runWith(Sink.head).futureValue
    }
  }
}
