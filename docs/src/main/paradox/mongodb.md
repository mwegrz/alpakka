# MongoDB

The MongoDB connector allows you to read and save documents.
You can query a stream of documents from @scala[@scaladoc[`MongoSource`](akka.stream.alpakka.mongodb.scaladsl.MongoSource$)]@java[@scaladoc[`MongoSource`](akka.stream.alpakka.mongodb.javadsl.MongoSource$)] or update documents in a collection with @scala[@scaladoc[`MongoSink`](akka.stream.alpakka.mongodb.scaladsl.MongoSink$)]@java[@scaladoc[`MongoSink`](akka.stream.alpakka.mongodb.javadsl.MongoSink$)].

This connector is based on the [Mongo Reactive Streams Driver](https://github.com/mongodb/mongo-java-driver-reactivestreams).

@@@ note { title="Alternative connector" }

Another MongoDB connector is available - ReactiveMongo.
It is a Scala driver that provides fully non-blocking and asynchronous I/O operations.
Please read more about it in the [ReactiveMongo documentation](http://reactivemongo.org).

@@@

@@project-info{ projectId="mongodb" }


## Artifacts

@@dependency [sbt,Maven,Gradle] {
  group=com.lightbend.akka
  artifact=akka-stream-alpakka-mongodb_$scala.binary.version$
  version=$project.version$
}

The table below shows direct dependencies of this module and the second tab shows all libraries it depends on transitively.

@@dependencies { projectId="mongodb" }


## Initialization

In the code examples below we will be using Mongo's support for automatic codec derivation for POJOs.
For Scala we will be using a case class and a macro based codec derivation.
For Java a codec for POJO is derived using reflection.

Scala
: @@snip [snip](/mongodb/src/test/scala/docs/scaladsl/MongoSourceSpec.scala) { #pojo }

Java
: @@snip [snip](/mongodb/src/test/java/docs/javadsl/Number.java) { #pojo }

For codec support, you first need to setup a @javadoc[`CodecRegistry`](org.bson.codecs.configuration.CodecRegistry).

Scala
: @@snip [snip](/mongodb/src/test/scala/docs/scaladsl/MongoSourceSpec.scala) { #codecs }

Java
: @@snip [snip](/mongodb/src/test/java/docs/javadsl/MongoSourceTest.java) { #codecs }

Sources provided by this connector need a prepared collection to communicate with the MongoDB server.
To get a reference to a collection, let's initialize a MongoDB connection and access the database.

Scala
: @@snip [snip](/mongodb/src/test/scala/docs/scaladsl/MongoSourceSpec.scala) { #init-connection }

Java
: @@snip [snip](/mongodb/src/test/java/docs/javadsl/MongoSourceTest.java) { #init-connection }

We will also need an @scaladoc[`ActorSystem`](akka.actor.ActorSystem) and an @scaladoc[`ActorMaterializer`](akka.stream.ActorMaterializer).

Scala
: @@snip [snip](/mongodb/src/test/scala/docs/scaladsl/MongoSourceSpec.scala) { #init-mat }

Java
: @@snip [snip](/mongodb/src/test/java/docs/javadsl/MongoSourceTest.java) { #init-mat }


## Source

Let's create a source from a Reactive Streams Publisher.

Scala
: @@snip [snip](/mongodb/src/test/scala/docs/scaladsl/MongoSourceSpec.scala) { #create-source }

Java
: @@snip [snip](/mongodb/src/test/java/docs/javadsl/MongoSourceTest.java) { #create-source }

And then run it.

Scala
: @@snip [snip](/mongodb/src/test/scala/docs/scaladsl/MongoSourceSpec.scala) { #run-source }

Java
: @@snip [snip](/mongodb/src/test/java/docs/javadsl/MongoSourceTest.java) { #run-source }

Here we used a basic sink to complete the stream by collecting all of the stream elements to a collection.
The power of streams comes from building larger data pipelines which leverage backpressure to ensure efficient flow control.
Feel free to edit the example code and build @extref[more advanced stream topologies](akka-docs:scala/stream/stream-introduction).

## Flow and Sink

Each of these sink factory methods have a corresponding factory in @scala[@scaladoc[`MongoFlow`](akka.stream.alpakka.mongodb.scaladsl.MongoFlow$)]@java[@scaladoc[`MongoFlow`](akka.stream.alpakka.mongodb.javadsl.MongoFlow$)] which will emit the written document or result of the operation downstream.

### Insert

We can use a Source of documents to save them to a mongo collection using @scala[@scaladoc[`MongoSink.insertOne`](akka.stream.alpakka.mongodb.scaladsl.MongoSink$)]@java[@scaladoc[`MongoSink.insertOne`](akka.stream.alpakka.mongodb.javadsl.MongoSink$)] or @scala[@scaladoc[`MongoSink.insertMany`](akka.stream.alpakka.mongodb.scaladsl.MongoSink$)]@java[@scaladoc[`MongoSink.insertMany`](akka.stream.alpakka.mongodb.javadsl.MongoSink$)].

Scala
: @@snip [snip](/mongodb/src/test/scala/docs/scaladsl/MongoSinkSpec.scala) { #insert-one }

Java
: @@snip [snip](/mongodb/src/test/java/docs/javadsl/MongoSinkTest.java) { #insert-one }

### Insert Many

Insert many can be used if you have a collection of documents to insert at once.

Scala
: @@snip [snip](/mongodb/src/test/scala/docs/scaladsl/MongoSinkSpec.scala) { #insert-many }

Java
: @@snip [snip](/mongodb/src/test/java/docs/javadsl/MongoSinkTest.java) { #insert-many }

### Update

We can update documents with a Source of @scaladoc[`DocumentUpdate`](akka.stream.alpakka.mongodb.DocumentUpdate) which is a filter and a update definition.
Use either @scala[@scaladoc[`MongoSink.updateOne`](akka.stream.alpakka.mongodb.scaladsl.MongoSink$)]@java[@scaladoc[`MongoSink.updateOne`](akka.stream.alpakka.mongodb.javadsl.MongoSink$)] or @scala[@scaladoc[`MongoSink.updateMany`](akka.stream.alpakka.mongodb.scaladsl.MongoSink$)]@java[@scaladoc[`MongoSink.updateMany`](akka.stream.alpakka.mongodb.javadsl.MongoSink$)] if the filter should target one or many documents.

Scala
: @@snip [snip](/mongodb/src/test/scala/docs/scaladsl/MongoSinkSpec.scala) { #update-one }

Java
: @@snip [snip](/mongodb/src/test/java/docs/javadsl/MongoSinkTest.java) { #update-one }

### Delete

We can delete documents with a Source of filters.
Use either @scala[@scaladoc[`MongoSink.deleteOne`](akka.stream.alpakka.mongodb.scaladsl.MongoSink$)]@java[@scaladoc[`MongoSink.deleteOne`](akka.stream.alpakka.mongodb.javadsl.MongoSink$)] or @scala[@scaladoc[`MongoSink.deleteMany`](akka.stream.alpakka.mongodb.scaladsl.MongoSink$)]@java[@scaladoc[`MongoSink.deleteMany`](akka.stream.alpakka.mongodb.javadsl.MongoSink$)] if the filter should target one or many documents.

Scala
: @@snip [snip](/mongodb/src/test/scala/docs/scaladsl/MongoSinkSpec.scala) { #delete-one }

Java
: @@snip [snip](/mongodb/src/test/java/docs/javadsl/MongoSinkTest.java) { #delete-one }
