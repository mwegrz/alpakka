/*
 * Copyright (C) 2016-2019 Lightbend Inc. <http://www.lightbend.com>
 */

package akka.stream.alpakka.jms

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, Supervision}
import akka.testkit.TestKit
import javax.jms._
import org.mockito.ArgumentMatchers.{any, anyBoolean, anyInt}
import org.mockito.Mockito.when
import org.scalatest._
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.mockito.MockitoSugar
import jmstestkit.JmsBroker

abstract class JmsSpec
    extends WordSpec
    with Matchers
    with BeforeAndAfterAll
    with BeforeAndAfterEach
    with ScalaFutures
    with Eventually
    with MockitoSugar {

  implicit val system = ActorSystem(this.getClass.getSimpleName)

  val decider: Supervision.Decider = ex => Supervision.Stop

  val settings = ActorMaterializerSettings(system).withSupervisionStrategy(decider)

  implicit val materializer = ActorMaterializer(settings)

  val consumerConfig = system.settings.config.getConfig(JmsConsumerSettings.configPath)
  val producerConfig = system.settings.config.getConfig(JmsProducerSettings.configPath)
  val browseConfig = system.settings.config.getConfig(JmsBrowseSettings.configPath)

  override protected def afterAll(): Unit =
    TestKit.shutdownActorSystem(system)

  def withConnectionFactory()(test: ConnectionFactory => Unit): Unit =
    withServer() { server =>
      test(server.createConnectionFactory)
    }

  def withServer()(test: JmsBroker => Unit): Unit = {
    val jmsBroker = JmsBroker()
    try {
      test(jmsBroker)
      Thread.sleep(500)
    } finally {
      if (jmsBroker.isStarted) {
        jmsBroker.stop()
      }
    }
  }

  def withMockedProducer(test: ProducerMock => Unit): Unit = test(ProducerMock())

  case class ProducerMock(factory: ConnectionFactory = mock[ConnectionFactory],
                          connection: Connection = mock[Connection],
                          session: Session = mock[Session],
                          producer: MessageProducer = mock[MessageProducer],
                          queue: javax.jms.Queue = mock[javax.jms.Queue]) {
    when(factory.createConnection()).thenReturn(connection)
    when(connection.createSession(anyBoolean(), anyInt())).thenReturn(session)
    when(session.createProducer(any[javax.jms.Destination])).thenReturn(producer)
    when(session.createQueue(any[String])).thenReturn(queue)
  }

  case class ConsumerMock(factory: ConnectionFactory = mock[ConnectionFactory],
                          connection: Connection = mock[Connection],
                          session: Session = mock[Session],
                          consumer: MessageConsumer = mock[MessageConsumer],
                          queue: javax.jms.Queue = mock[javax.jms.Queue]) {
    when(factory.createConnection()).thenReturn(connection)
    when(connection.createSession(anyBoolean(), anyInt())).thenReturn(session)
    when(session.createConsumer(any[javax.jms.Destination])).thenReturn(consumer)
    when(session.createQueue(any[String])).thenReturn(queue)
  }

  def withMockedConsumer(test: ConsumerMock => Unit): Unit = test(ConsumerMock())

}
