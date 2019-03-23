/*
 * Copyright (C) 2016-2019 Lightbend Inc. <http://www.lightbend.com>
 */

package akka.stream.alpakka.jms.impl

import akka.annotation.InternalApi
import akka.stream.alpakka.jms.{AcknowledgeMode, Destination, JmsConsumerSettings, TxEnvelope}
import akka.stream.stage.{GraphStageLogic, GraphStageWithMaterializedValue}
import akka.stream.{Attributes, Outlet, SourceShape}
import javax.jms

import scala.concurrent.{Await, TimeoutException}

/**
 * Internal API.
 */
@InternalApi
private[jms] final class JmsTxSourceStage(settings: JmsConsumerSettings, destination: Destination)
    extends GraphStageWithMaterializedValue[SourceShape[TxEnvelope], JmsConsumerMatValue] {

  private val out = Outlet[TxEnvelope]("JmsSource.out")

  override def shape: SourceShape[TxEnvelope] = SourceShape[TxEnvelope](out)

  override protected def initialAttributes: Attributes = Attributes.name("JmsTxConsumer")

  override def createLogicAndMaterializedValue(
      inheritedAttributes: Attributes
  ): (GraphStageLogic, JmsConsumerMatValue) = {
    val logic = new SourceStageLogic[TxEnvelope](shape, out, settings, destination, inheritedAttributes) {
      protected def createSession(connection: jms.Connection,
                                  createDestination: jms.Session => javax.jms.Destination) = {
        val session =
          connection.createSession(true, settings.acknowledgeMode.getOrElse(AcknowledgeMode.SessionTransacted).mode)
        new JmsConsumerSession(connection, session, createDestination(session), destination)
      }

      protected def pushMessage(msg: TxEnvelope): Unit = push(out, msg)

      override protected def onSessionOpened(jmsSession: JmsConsumerSession): Unit =
        jmsSession match {
          case session: JmsSession =>
            session
              .createConsumer(settings.selector)
              .map { consumer =>
                consumer.setMessageListener(new jms.MessageListener {

                  def onMessage(message: jms.Message): Unit =
                    try {
                      val envelope = TxEnvelope(message, session)
                      handleMessage.invoke(envelope)
                      val action = Await.result(envelope.commitFuture, settings.ackTimeout)
                      action()
                    } catch {
                      case _: TimeoutException => session.session.rollback()
                      case e: IllegalArgumentException => handleError.invoke(e) // Invalid envelope. Fail the stage.
                      case e: jms.JMSException => handleError.invoke(e)
                    }
                })
              }
              .onComplete(sessionOpenedCB.invoke)

          case _ =>
            throw new IllegalArgumentException(
              "Session must be of type JMSAckSession, it is a " +
              jmsSession.getClass.getName
            )
        }
    }

    (logic, logic.consumerControl)
  }
}
