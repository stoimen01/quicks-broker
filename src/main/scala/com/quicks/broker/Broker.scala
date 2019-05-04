package com.quicks.broker

import java.util.Base64

import akka.NotUsed
import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.ws.Message
import akka.http.scaladsl.model.{ContentTypes, HttpMethods, HttpRequest}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.settings.ServerSettings
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.{ActorMaterializer, OverflowStrategy}
import com.quicks.broker.ConnectionAgent._

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.io.StdIn

object Broker {

  def main(args: Array[String]) {

    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()
    implicit val executionContext = system.dispatcher
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
      get {
        path("ws") {
          handleWebSocketMessages(newUser())
        }
      } ~ get {
        path("ice") {
          val request = HttpRequest(uri = "https://global.xirsys.net/_turn/Quicks")
            .withMethod(HttpMethods.PUT)
            .addHeader(RawHeader(
              "Authorization",
              "Basic " + Base64.getEncoder.encodeToString(sys.env("XIRSYS_TOKEN").getBytes()))
            )
            .withEntity(ContentTypes.`application/json`, "{\"format\": \"urls\"}")
          onSuccess(Http().singleRequest(request)) { it =>
            complete(it)
          }
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
