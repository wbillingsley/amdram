package com.wbillingsley.amdram

import scala.concurrent.*
import scala.collection.immutable.{Queue, Set}



trait Actor[T](inbox:Inbox[T]) {

    def self:Recipient[T] = inbox

    /** An Actor is idle if it has nothing to do. */
    final def idle = inbox.isEmpty

    @volatile private var currentMessage:Option[T] = None

    @volatile private var alive = true

    @volatile private var scheduled = false

    def busy:Boolean  = synchronized { currentMessage.nonEmpty }

    def stop():Unit = synchronized { alive = false }

    def start()(using ac:ActorContext[T], ec:ExecutionContext):Unit = synchronized { 
        alive = true
        schedule()
    }

    def schedule()(using ac:ActorContext[T], ec:ExecutionContext):Unit = synchronized {
        if !scheduled then
            scheduled = true
            ec.execute(() => workLoop())
    }

    /** Takes a task from the queue and performs it. */
    private def workLoop()(using context:ActorContext[T], ec:ExecutionContext):Unit = {
        currentMessage = synchronized {
            scheduled = false
            if inbox.isEmpty then None else Some(inbox.pop())
        }

        for m <- currentMessage do receive(m)

        // TODO: Make this only schedule work if there's more to do (but that requires our inbox to wake us up if new work comes in)
        if alive then schedule()
    }

    def receive(message:T):ActorContext[T] ?=> Unit
}

object Actor {

    def constant[T](f: T => ActorContext[T] ?=> Unit):Actor[T] = {
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
            
            override def receive(msg:T) = (ac:ActorContext[T]) ?=>
                this.handler = handler.receive(msg) match {
                    case _:Unit => this.handler
                    case mh:MessageHandler[T] @unchecked => mh
                }
        }
    }

    def context[T](actor:Actor[T], group:Troupe):ActorContext[T] = new ActorContext {
        def self = actor.self

        def terminate() = 
            actor.stop()

        export group.spawn
        export group.spawnLoop
    }

}

extension [M] (a:Recipient[M]) {

    inline def ask[T, R](message:M)(using ag:ActorContext[T]):Future[R] = {
        val p = Promise[R]
        ag.spawnLoop[R] { reply =>
            reply match {
                case r:R => p.success(reply)
                case other:Any => p.failure(IllegalArgumentException(s"Unexpected reply $other"))
            }
            ag.terminate()
        }
        a.send(message)
        p.future
    }

    inline def ?[T, R](message:M)(using ag:ActorContext[T]):Future[R] = ask(message)(using ag)

}