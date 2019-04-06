package broker

import akka.actor.AbstractActor
import akka.actor.ActorRef
import akka.actor.Props
import akka.actor.Terminated
import akka.http.javadsl.model.ws.Message

class ConnectionsManager : AbstractActor() {

    object Join

    var connections = emptySet<ActorRef>()

    override fun createReceive(): Receive {
        return receiveBuilder()
            .match(Join::class.java) {
                connections = connections + sender
                context.watch(sender)
            }
            .match(Terminated::class.java) {
                connections = connections - it.actor
            }
            .match(Message::class.java) { msg ->
                connections.forEach { conn ->
                    if (conn != sender) {
                        conn.tell(msg, self)
                    }
                }
            }
            .build()
    }

    companion object {
        fun props(): Props =
            Props.create(ConnectionsManager::class.java) { ConnectionsManager() }
    }
}