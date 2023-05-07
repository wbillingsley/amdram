package com.wbillingsley.amdram
import scala.collection.immutable.{Queue, Set}

class Inbox[T](onReceive: T => Unit = { (_:T) => () }) extends Recipient[T] {
    /** The actor's inbox of messages to respond to */
    @volatile private var queue:Queue[T] = Queue.empty

    def isEmpty = queue.isEmpty

    def size = queue.size

    /** Sends this actor a message, putting it into its inbox. */
    override def send(message:T):Unit = {
        synchronized {
            queue = queue.enqueue(message)
            onReceive(message)
        }
    }

    def pop():T = synchronized { 
        val (m, q) = queue.dequeue
        queue = q
        m
    }

}
