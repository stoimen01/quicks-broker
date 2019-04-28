package com.quicks.broker

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.http.scaladsl.model.ws.Message
import com.quicks.broker.ConnectionAgent.SenderReceived

object ConnectionAgent {
  case class SenderReceived(sender: ActorRef)
}

class ConnectionAgent(connectionsManager: ActorRef) extends Actor with ActorLogging {

  private var senderActor: ActorRef = _

  override def receive = {

    case m: Message =>
      log.info("Processing message...")
      if (sender() == connectionsManager) {
        if (senderActor != null) {
          senderActor ! m
        } else {
          log.warning("Message was not sent !")
        }
      } else {
        connectionsManager ! m
      }

    case SenderReceived(sender) =>
      senderActor = sender
      connectionsManager ! Join

  }

}