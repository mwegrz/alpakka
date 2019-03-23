/*
 * Copyright (C) 2016-2019 Lightbend Inc. <http://www.lightbend.com>
 */

package akka.stream.alpakka.sqs.javadsl;

import akka.actor.ActorSystem;
import akka.stream.ActorMaterializer;
import akka.stream.Materializer;
import akka.testkit.javadsl.TestKit;
// #init-client

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

// #init-client
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;

import java.net.URI;
import java.util.Random;

public abstract class BaseSqsTest {

  protected static ActorSystem system;
  protected static Materializer materializer;

  private boolean initialized = false;
  protected String sqsEndpoint = "http://localhost:9324";
  protected SqsAsyncClient sqsClient;

  @BeforeClass
  public static void setup() {
    // #init-mat
    system = ActorSystem.create();
    materializer = ActorMaterializer.create(system);
    // #init-mat
  }

  @AfterClass
  public static void teardown() {
    TestKit.shutdownActorSystem(system);
  }

  @Before
  public void setupBefore() {
    if (!initialized) {
      sqsClient = createAsyncClient(sqsEndpoint);
      initialized = true;
    }
  }

  private SqsAsyncClient createAsyncClient(String sqsEndpoint) {
    // #init-client
    SqsAsyncClient sqsClient =
        SqsAsyncClient.builder()
            .credentialsProvider(
                StaticCredentialsProvider.create(AwsBasicCredentials.create("x", "x")))
            .endpointOverride(URI.create(sqsEndpoint))
            .region(Region.EU_CENTRAL_1)
            .build();

    system.registerOnTermination(() -> sqsClient.close());
    // #init-client
    return sqsClient;
  }

  protected String randomQueueUrl() throws Exception {
    return sqsClient
        .createQueue(
            CreateQueueRequest.builder()
                .queueName(String.format("queue-%s", new Random().nextInt()))
                .build())
        .get()
        .queueUrl();
  }
}
