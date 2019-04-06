package broker

import akka.actor.AbstractActor
import akka.actor.ActorRef
import akka.actor.Props
import akka.event.Logging
import akka.http.javadsl.model.ws.Message
import akka.http.javadsl.model.ws.TextMessage

class ConnectionActor(
    private val sinkActor: ActorRef,
    private val connectionsManager: ActorRef
) : AbstractActor() {

    private val log = Logging.getLogger(context.system, this)

    override fun createReceive(): Receive {
        connectionsManager.tell(ConnectionsManager.Join, self)
        return receiveBuilder()
            .match(Message::class.java) {

                log.info("Processing message...")

                if (sender == connectionsManager) {
                    sinkActor.tell(it, self)
                } else {
                    connectionsManager.tell(it, self)
                }

            }
            .build()
    }

    companion object {
        fun props(sink: ActorRef, connectionsManager: ActorRef): Props =
            Props.create(ConnectionActor::class.java) { ConnectionActor(sink, connectionsManager) }
    }

}