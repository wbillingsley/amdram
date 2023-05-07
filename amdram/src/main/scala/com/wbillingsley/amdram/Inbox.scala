package com.wbillingsley.amdram
import scala.collection.immutable.{Queue, Set}

import java.util.concurrent.ConcurrentLinkedQueue

class Inbox[T](onReceive: T => Unit = { (_:T) => () }) extends Recipient[T] {
    /** The actor's inbox of messages to respond to */
    private val queue:ConcurrentLinkedQueue[T] = ConcurrentLinkedQueue()

    def isEmpty = queue.isEmpty

    def size = queue.size

    /** Sends this actor a message, putting it into its inbox. */
    override def send(message:T):Unit = {
        queue.add(message)
        onReceive(message)
    }

    def pop():Option[T] = {
        Option(queue.poll())
    }

}
