package broker

import akka.actor.ActorSystem
import akka.actor.PoisonPill
import akka.http.javadsl.ConnectHttp
import akka.http.javadsl.Http
import akka.http.javadsl.model.HttpRequest
import akka.http.javadsl.model.HttpResponse
import akka.http.javadsl.model.StatusCodes
import akka.http.javadsl.model.ws.Message
import akka.http.javadsl.model.ws.UpgradeToWebSocket
import akka.stream.ActorMaterializer
import akka.stream.OverflowStrategy
import akka.stream.javadsl.Sink
import akka.stream.javadsl.Source

fun main() {

    val system = ActorSystem.create("quicks-broker")

    try {

        val mat = ActorMaterializer.create(system)

        val connectionsManager = system.actorOf(ConnectionsManager.props(), "c-manager")

        val serverBindingFuture = Http.get(system)
            .bindAndHandleSync({ request ->

                if (request.uri.path() != "/ws") {
                    request.discardEntityBytes(mat)
                    return@bindAndHandleSync HttpResponse.create()
                        .withStatus(StatusCodes.NOT_FOUND)
                        .withEntity("Unknown resource !")
                }

                val header = request.wsUpgradeHeader() ?:
                return@bindAndHandleSync HttpResponse.create()
                    .withStatus(StatusCodes.BAD_REQUEST)
                    .withEntity("Ws upgrade header not found !")

                // getting the actor responsible for sending outgoing messages
                val source = Source
                    .actorRef<Message>(10, OverflowStrategy.fail())
                    .preMaterialize(mat)

                // passing the source actor to the connection actor to enable sending messages from it
                val connectionActor = system.actorOf(ConnectionActor.props(source.first(), connectionsManager))

                // routing incoming messages to the connection actor
                val sink = Sink.actorRef<Message>(connectionActor, PoisonPill.getInstance())

                return@bindAndHandleSync header.handleMessagesWith(sink, source.second())

        }, ConnectHttp.toHost("127.0.0.1", 8080), mat)

        println("Press ENTER to exit the system")
        readLine()

        serverBindingFuture
            .thenCompose { it.unbind() }
            .thenAccept { system.terminate() }

    } finally {
        system.terminate()
    }
}

fun HttpRequest.wsUpgradeHeader(): UpgradeToWebSocket? {
    return headers.firstOrNull { it is UpgradeToWebSocket } as? UpgradeToWebSocket
}

