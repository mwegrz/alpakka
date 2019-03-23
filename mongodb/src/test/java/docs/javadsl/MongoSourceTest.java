/*
 * Copyright (C) 2016-2019 Lightbend Inc. <http://www.lightbend.com>
 */

package docs.javadsl;

import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.stream.ActorMaterializer;
import akka.stream.Materializer;
import akka.stream.alpakka.mongodb.javadsl.MongoSource;
import akka.stream.javadsl.Source;
import akka.stream.javadsl.Sink;
import akka.stream.testkit.javadsl.StreamTestKit;
import com.mongodb.reactivestreams.client.*;
import org.bson.Document;
import org.bson.codecs.ValueCodecProvider;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;

public class MongoSourceTest {

  private static ActorSystem system;
  private static Materializer mat;

  private final MongoClient client;
  private final MongoDatabase db;
  private final MongoCollection<Document> numbersDocumentColl;
  private final MongoCollection<Number> numbersColl;

  public MongoSourceTest() {
    // #init-mat
    system = ActorSystem.create();
    mat = ActorMaterializer.create(system);
    // #init-mat

    // #codecs
    PojoCodecProvider codecProvider = PojoCodecProvider.builder().register(Number.class).build();
    CodecRegistry codecRegistry =
        CodecRegistries.fromProviders(codecProvider, new ValueCodecProvider());
    // #codecs

    // #init-connection
    client = MongoClients.create("mongodb://localhost:27017");
    db = client.getDatabase("MongoSourceTest");
    numbersColl = db.getCollection("numbers", Number.class).withCodecRegistry(codecRegistry);
    // #init-connection

    numbersDocumentColl = db.getCollection("numbers");
  }

  @Before
  public void cleanDb() throws Exception {
    Source.fromPublisher(numbersDocumentColl.deleteMany(new Document()))
        .runWith(Sink.head(), mat)
        .toCompletableFuture()
        .get(5, TimeUnit.SECONDS);
  }

  @After
  public void checkForLeaks() throws Exception {
    Source.fromPublisher(numbersDocumentColl.deleteMany(new Document()))
        .runWith(Sink.head(), mat)
        .toCompletableFuture()
        .get(5, TimeUnit.SECONDS);
    StreamTestKit.assertAllStagesStopped(mat);
  }

  @AfterClass
  public static void terminateActorSystem() {
    system.terminate();
  }

  @Test
  public void streamTheResultOfASimpleMongoQuery() throws Exception {
    List<Integer> data = seed();

    final Source<Document, NotUsed> source = MongoSource.create(numbersDocumentColl.find());
    final CompletionStage<List<Document>> rows = source.runWith(Sink.seq(), mat);

    assertEquals(
        data,
        rows.toCompletableFuture()
            .get(5, TimeUnit.SECONDS)
            .stream()
            .map(n -> n.getInteger("_id"))
            .collect(Collectors.toList()));
  }

  @Test
  public void supportCodecRegistryToReadClassObjects() throws Exception {
    List<Number> data = seed().stream().map(Number::new).collect(Collectors.toList());

    // #create-source
    final Source<Number, NotUsed> source = MongoSource.create(numbersColl.find(Number.class));
    // #create-source

    // #run-source
    final CompletionStage<List<Number>> rows = source.runWith(Sink.seq(), mat);
    // #run-source

    assertEquals(data, rows.toCompletableFuture().get(5, TimeUnit.SECONDS));
  }

  @Test
  public void supportMultipleMaterializations() throws Exception {
    final List<Integer> data = seed();

    final Source<Document, NotUsed> source = MongoSource.create(numbersDocumentColl.find());

    assertEquals(
        data,
        source
            .runWith(Sink.seq(), mat)
            .toCompletableFuture()
            .get(5, TimeUnit.SECONDS)
            .stream()
            .map(n -> n.getInteger("_id"))
            .collect(Collectors.toList()));
    assertEquals(
        data,
        source
            .runWith(Sink.seq(), mat)
            .toCompletableFuture()
            .get(5, TimeUnit.SECONDS)
            .stream()
            .map(n -> n.getInteger("_id"))
            .collect(Collectors.toList()));
  }

  @Test
  public void streamTheResultOfMongoQueryThatResultsInNoData() throws Exception {
    final Source<Document, NotUsed> source = MongoSource.create(numbersDocumentColl.find());

    assertEquals(
        true,
        source.runWith(Sink.seq(), mat).toCompletableFuture().get(5, TimeUnit.SECONDS).isEmpty());
  }

  private List<Integer> seed() throws Exception {
    final List<Integer> numbers = IntStream.range(1, 10).boxed().collect(Collectors.toList());

    final List<Document> documents =
        numbers.stream().map(i -> Document.parse("{_id:" + i + "}")).collect(Collectors.toList());

    final CompletionStage<Success> completion =
        Source.fromPublisher(numbersDocumentColl.insertMany(documents)).runWith(Sink.head(), mat);
    completion.toCompletableFuture().get(5, TimeUnit.SECONDS);

    return numbers;
  }
}
