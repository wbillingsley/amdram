package com.wbillingsley.amdram

import scala.concurrent.*
import scala.collection.immutable.{Queue, Set}


/** Something you can send messages to */
trait Recipient[-T] {
    def send(message:T):Unit

    def !(message:T):Unit = send(message)
}


/**
  * A group of actors schedules its work on a particular execution context
  *
  * @param ec
  */
class ActorGroup(using ec:ExecutionContext) {
  
    private var actors:Set[Actor[_]] = Set.empty

    def spawnLoop[T](f: T => ActorContext ?=> Unit):Recipient[T] = 
                    val a = Actor.constant(f)
                    enlist(a)
                    a.self

    def spawn[T](handler:MessageHandler[T]):Recipient[T] = 
        val a = Actor.receive(handler)
        enlist(a)
        a.self

    /** 
     * The workloop for an ActorSystem asks all its actors to process a message.
     * How frequently this is called is left for implementors - for instance in Scala.js we will sometimes
     * find it helpful to schedule it in an animator to make work visible.
     */
    def workLoop():Unit = {
        for actor <- actors if !actor.idle do
            given context:ActorContext = new ActorContext {

                export ActorGroup.this.spawn

                export ActorGroup.this.spawnLoop

                def terminate():Unit = 
                    delist(actor)
            }
            ec.execute(() => actor.workLoop())
    }

    def enlist[T](actor:Actor[T]):Unit = 
        synchronized {
            actors = actors + actor
        }

    def delist[T](actor:Actor[T]):Unit = 
        synchronized {
            actors = actors - actor
        }

}

trait ActorContext {

    def spawnLoop[T](f: T => ActorContext ?=> Unit):Recipient[T]

    def spawn[T](handler:MessageHandler[T]):Recipient[T]

    def terminate():Unit

}


/**
  * Fundamental to Actors in Erlang is what to do when you receive a message.
  * From the ping-pong example in the Erlang docs, we also need to enable the
  * case where an Actor changes to a different message handler at the end -
  * so we'll give receive a return type of the next MessageHandler.
  */
trait MessageHandler[-T] {
    def receive(message:T)(using ac: ActorContext): MessageHandler[T] | Unit
}

object MessageHandler {
    def apply[T](f: T => ActorContext ?=> MessageHandler[T] | Unit) = {
        new MessageHandler[T] {
            override def receive(message:T)(using ac:ActorContext) = 
                f.apply(message).apply
        }     
    }
}


class Inbox[T] extends Recipient[T] {
    /** The actor's inbox of messages to respond to */
    private var queue:Queue[T] = Queue.empty

    def isEmpty = queue.isEmpty

    def size = queue.size

    /** Sends this actor a message, putting it into its inbox. */
    override def send(message:T):Unit = {
        synchronized {
            queue = queue.enqueue(message)
        }
    }

    def pop():T = synchronized { 
        val (m, q) = queue.dequeue
        queue = q
        m
    }

}

trait Actor[-T](inbox:Inbox[T]) {

    def self:Recipient[T] = inbox

    /** An Actor is idle if it has nothing to do. */
    final def idle = inbox.isEmpty

    /** Takes a task from the queue and performs it. */
    final def workLoop()(using context:ActorContext):Unit = {
        // We don't run actors in parallel, so we just need to keep ourselves synchronised with any additions
        if !inbox.isEmpty then receive(inbox.pop())
    }

    def receive(message:T):ActorContext ?=> Unit
}

object Actor {

    def constant[T](f: T => ActorContext ?=> Unit):Actor[T] = {
        val inbox = new Inbox[T]
        new Actor[T](inbox) {
            def receive(msg:T) = 
                f(msg)
        }
    }

    def receive[T](h:MessageHandler[T]):Actor[T] = {
        val inbox = new Inbox[T]
        new Actor(inbox) {
            var handler:MessageHandler[T] = h
            
            override def receive(msg:T) = (ac:ActorContext) ?=>
                this.handler = handler.receive(msg) match {
                    case _:Unit => this.handler
                    case mh:MessageHandler[T] @unchecked => mh
                }
        }
    }

}

extension [T] (a:Actor[T]) {

    def ask(message:T)(using ag:ActorContext):Future[Any] = {
        val p = Promise[Any]
        ag.spawnLoop[Any] { reply =>
            p.success(reply)
            ag.terminate()
        }
        p.future
    }


}