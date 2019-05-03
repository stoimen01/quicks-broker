package com.quicks.broker

import java.util.concurrent.atomic.AtomicInteger

import akka.NotUsed
import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ws.Message
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.settings.ServerSettings
import akka.util.ByteString
import com.quicks.broker.ConnectionAgent.{Ack, Complete, Error, Init, SenderReceived}
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration._
import scala.concurrent.Await
import scala.io.StdIn

object Broker {

  def main(args: Array[String]) {

    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()
    val serverSettings = ServerSettings(system)

    val customWebsocketSettings =
      serverSettings.websocketSettings
      .withPeriodicKeepAliveMaxIdle(1.second)

    val customSettings =
      serverSettings.withWebsocketSettings(customWebsocketSettings)

    val connManager = system.actorOf(Props[ConnectionsManager])

    def newUser(): Flow[Message, Message, NotUsed] = {

      val connAgent = system.actorOf(Props(new ConnectionAgent(connManager)))

      val in: Sink[Message, NotUsed] = Sink.actorRefWithAck(
        connAgent,
        Init,
        Ack,
        Complete,
        it => Error(it)
      )

      val out = Source.actorRef(10, OverflowStrategy.fail)
        .mapMaterializedValue( sender => {
          connAgent ! SenderReceived(sender)
          NotUsed
        })

      Flow.fromSinkAndSource(in, out)
    }

    val route =
      path("ws") {
        get {
          handleWebSocketMessages(newUser())
        }
      }

    val binding = Await.result(Http()
      .bindAndHandle(route, "127.0.0.1", 8080, settings = customSettings), 3.seconds)

    // the rest of the sample code will go here
    println("Started server at 127.0.0.1:8080, press enter to kill server")
    StdIn.readLine()
    system.terminate()

  }

}
