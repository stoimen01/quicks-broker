package com.quicks.broker

import akka.actor.{Actor, ActorLogging, ActorRef, Terminated}
import akka.http.scaladsl.model.ws.Message

object Join

class ConnectionsManager extends Actor with ActorLogging {

  var connections: Set[ActorRef] = Set.empty

  override def receive = {

    case Join =>
      log.info("New connection joined !")
      connections += sender()
      context.watch(sender())

    case Terminated(conn) =>
      log.info("Connection was terminated !")
      connections -= conn

    case m: Message =>
      log.info("Broadcasting message !")
      connections.foreach( conn => {
        if (conn != sender()) {
          conn ! m
        }
      })

  }

}
