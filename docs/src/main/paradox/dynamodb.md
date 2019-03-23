# AWS DynamoDB

The AWS DynamoDB connector provides a flow for streaming DynamoDB requests. For more information about DynamoDB please visit the [official documentation](https://aws.amazon.com/dynamodb/).

@@project-info{ projectId="dynamodb" }

## Artifacts

@@dependency [sbt,Maven,Gradle] {
  group=com.lightbend.akka
  artifact=akka-stream-alpakka-dynamodb_$scala.binary.version$
  version=$project.version$
}

The table below shows direct dependencies of this module and the second tab shows all libraries it depends on transitively.

@@dependencies { projectId="dynamodb" }


## Setup

Factories provided in the @scaladoc[DynamoDb](akka.stream.alpakka.dynamodb.scaladsl.DynamoDb$) will use the client managed by the extension. The managed client will be created using the configuration resolved from `reference.conf` and with overrides from `application.conf`.

Example `application.conf`
: @@snip [snip](/dynamodb/src/test/scala/akka/stream/alpakka/dynamodb/DynamoSettingsSpec.scala) { #static-creds }

`reference.conf`
: @@snip [snip](/dynamodb/src/main/resources/reference.conf)

If the credentials are not set in the configuration, connector will use the [default credential provider chain](http://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/credentials.html) provided by the [DynamoDB Java SDK](http://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/basics.html) to retrieve credentials.

## Usage

For simple operations you can issue a single request, and get back the result in a @scala[`Future`]@java[`CompletionStage`].

Scala
: @@snip [snip](/dynamodb/src/test/scala/docs/scaladsl/ExampleSpec.scala) { #simple-request }

Java
: @@snip [snip](/dynamodb/src/test/java/docs/javadsl/ExampleTest.java) { #simple-request }

You can also get the response to a request as an element emitted from a Flow:

Scala
: @@snip [snip](/dynamodb/src/test/scala/docs/scaladsl/ExampleSpec.scala) { #flow }

Java
: @@snip [snip](/dynamodb/src/test/java/docs/javadsl/ExampleTest.java) { #flow }

Some DynamoDB operations, such as Query and Scan, are paginated by nature.
This is how you can get a stream of all result pages:

Scala
: @@snip [snip](/dynamodb/src/test/scala/docs/scaladsl/ExampleSpec.scala) { #paginated }

Java
: @@snip [snip](/dynamodb/src/test/java/docs/javadsl/ExampleTest.java) { #paginated }

A custom configured client can be used by attaching it as an attribute to the stream:

Scala
: @@snip [snip](/dynamodb/src/test/scala/docs/scaladsl/ExampleSpec.scala) { #attributes }

Java
: @@snip [snip](/dynamodb/src/test/java/docs/javadsl/ExampleTest.java) { #attributes }


## Running the example code

The code in this guide is part of runnable tests of this project. You are welcome to edit the code and run it in sbt.

> Test code requires DynamoDB running in the background. You can start one quickly using docker:
>
> `docker-compose up dynamodb`

Scala
:   ```
    sbt
    > dynamodb/testOnly *Spec
    ```

Java
:   ```
    sbt
    > dynamodb/testOnly *Test
    ```
