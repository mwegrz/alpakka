# AWS SNS

The AWS SNS connector provides an Akka Stream Flow and Sink for push notifications through AWS SNS.

For more information about AWS SNS please visit the [official documentation](https://aws.amazon.com/documentation/sns/).

@@project-info{ projectId="sns" }

## Artifacts

@@dependency [sbt,Maven,Gradle] {
  group=com.lightbend.akka
  artifact=akka-stream-alpakka-sns_$scala.binary.version$
  version=$project.version$
}

The table below shows direct dependencies of this module and the second tab shows all libraries it depends on transitively.

@@dependencies { projectId="sns" }


## Setup

Sources provided by this connector need a prepared `AmazonSNSAsyncClient` to publish messages to a topic.

Scala
: @@snip [snip](/sns/src/test/scala/akka/stream/alpakka/sns/IntegrationTestContext.scala) { #init-client }

Java
: @@snip [snip](/sns/src/test/java/docs/javadsl/SnsPublisherTest.java) { #init-client }

We will also need an @scaladoc[ActorSystem](akka.actor.ActorSystem) and an @scaladoc[ActorMaterializer](akka.stream.ActorMaterializer).

Scala
: @@snip [snip](/sns/src/test/scala/akka/stream/alpakka/sns/IntegrationTestContext.scala) { #init-system }

Java
: @@snip [snip](/sns/src/test/java/docs/javadsl/SnsPublisherTest.java) { #init-system }

This is all preparation that we are going to need.

## Publish messages to an SNS topic

Now we can publish a message to any SNS topic where we have access to by providing the topic ARN to the
@scaladoc[SnsPublisher](akka.stream.alpakka.sns.scaladsl.SnsPublisher$) Flow or Sink factory method.

### Using a Flow

Scala
: @@snip [snip](/sns/src/test/scala/docs/scaladsl/SnsPublisherSpec.scala) { #use-flow }

Java
: @@snip [snip](/sns/src/test/java/docs/javadsl/SnsPublisherTest.java) { #use-flow }

As you can see, this would publish the messages from the source to the specified AWS SNS topic.
After a message has been successfully published, a
[PublishResult](http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/sns/model/PublishResult.html)
will be pushed downstream.

### Using a Sink

Scala
: @@snip [snip](/sns/src/test/scala/docs/scaladsl/SnsPublisherSpec.scala) { #use-sink }

Java
: @@snip [snip](/sns/src/test/java/docs/javadsl/SnsPublisherTest.java) { #use-sink }

As you can see, this would publish the messages from the source to the specified AWS SNS topic.
