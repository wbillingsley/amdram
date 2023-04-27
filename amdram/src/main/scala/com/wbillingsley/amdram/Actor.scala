package com.wbillingsley.amdram

import scala.concurrent.ExecutionContext
import scala.collection.immutable.Queue

/**
  * A group of actors schedules its work on a particular execution context
  *
  * @param ec
  */
class ActorGroup(using ec:ExecutionContext) {
  
    private var actors:Queue[Actor[_]] = Queue.empty

    /** 
     * The workloop for an ActorSystem asks all its actors to process a message.
     * How frequently this is called is left for implementors - for instance in Scala.js we will sometimes
     * find it helpful to schedule it in an animator to make work visible.
     */
    def workLoop():Unit = {
        for actor <- actors if !actor.idle do ec.execute(() => actor.workLoop())
    }

    def enlist[T](actor:Actor[T]):Unit = 
        synchronized {
            actors = actors.enqueue(actor)
        }

}

trait Actor[-T] {

    /** The actor's inbox of messages to respond to */
    private var queue:Queue[T] = Queue.empty

    /** An Actor is idle if it has nothing to do. */
    final def idle = queue.isEmpty

    /** Sends this actor a message, putting it into its inbox. */
    final def send(message:T):Unit = {
        synchronized {
            queue = queue.enqueue(message)
        }
    }

    /** Takes a task from the queue and performs it. */
    final def workLoop():Unit = {
        // We don't run actors in parallel, so we just need to keep ourselves synchronised with any additions
        if !queue.isEmpty then
            val message = synchronized { 
                val (m, q) = queue.dequeue
                queue = q
                m
            }
            receive(message)
    }

    /** Defined by the user - performs the work. */
    def receive(message: T):Unit
}