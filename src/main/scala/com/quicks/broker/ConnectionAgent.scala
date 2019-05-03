package com.quicks.broker

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.http.scaladsl.model.ws.Message
import com.quicks.broker.ConnectionAgent.SenderReceived

object ConnectionAgent {
  object Ack
  object Init
  object Complete
  case class Error(throwable: Throwable)
  case class SenderReceived(sender: ActorRef)
}

class ConnectionAgent(connectionsManager: ActorRef) extends Actor with ActorLogging {

  private var senderActor: ActorRef = _

  override def receive = {

    case m: Message =>
      log.info("Processing message...")
      sender() ! ConnectionAgent.Ack
      if (sender() == connectionsManager) {
        if (senderActor != null) {
          senderActor ! m
        } else {
          log.warning("Message was not sent !")
        }
      } else {
        connectionsManager ! m
      }

    case err: ConnectionAgent.Error =>
      log.error("ERR: {}", err.throwable)

    case ConnectionAgent.Complete =>
      log.info("WS COMPLETED")

    case SenderReceived(sender) =>
      senderActor = sender
      connectionsManager ! Join

  }

}