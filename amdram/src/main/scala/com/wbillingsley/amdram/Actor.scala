package com.wbillingsley.amdram

import scala.concurrent.*
import scala.collection.immutable.{Queue, Set}

import scala.util.Random


trait Actor[T] {

    val _id:String = Random.alphanumeric.take(12).mkString

    def inbox:Inbox[T] 

    def self:Recipient[T] = inbox

    def idle:Boolean = inbox.isEmpty

    @volatile private var currentMessage:Option[T] = None

    @volatile private var alive = true

    @volatile private var scheduled = false

    def busy:Boolean  = currentMessage.nonEmpty

    def stop():Unit = { 
        alive = false 
    }

    def start()(using ac:ActorContext[T], ec:ExecutionContext):Unit = { 
        schedule()
    }

    def schedule()(using ac:ActorContext[T], ec:ExecutionContext):Unit = {

        // Note - at the moment, we don't care if we've already been scheduled.
        // The workloop only does serious work if it finds a message, so a few excess workloops is cheap.

        ec.execute(() => workLoop())
        scheduled = true
    }

    /** Takes a task from the queue and performs it. */
    private def workLoop()(using context:ActorContext[T], ec:ExecutionContext):Unit = synchronized {
        // println(s"$_id entered workloop")
        scheduled = false // We can no longer be sure future workloops have been scheduled

        inbox.pop() match {
            case Some(m) => 
                // println(s"$_id processing $m")
                receive(m)
            case None => 
                // println(s"$_id's inbox was empty")
        }

        // TODO: Make this only schedule work if there's more to do (but that requires our inbox to wake us up if new work comes in)
        if alive && !inbox.isEmpty then schedule()
    }

    def receive(message:T):ActorContext[T] ?=> Unit
}

object Actor {

    def context[T](actor:Actor[T], group:Troupe):ActorContext[T] = new ActorContext {
        def self = actor.self

        def terminate() = 
            actor.stop()

        export group.spawn
        export group.spawnLoop
    }

}

class OneTime[R] extends MessageHandler[R] {
    private val p = Promise[R]

    override def receive(message:R)(using ac: ActorContext[R]) = {
        p.success(message)
        ac.terminate()
    }

    def future = p.future
}


extension [M] (a:Recipient[M]) {

    inline def ask[T, R](message: Recipient[R] => M)(using sm:SpawnMethods):Future[R] = {
        val ot = OneTime[R]
        val replyActor = sm.spawn(ot)
        a.send(message(replyActor))
        ot.future
    }

    inline def ?[T, R](message:Recipient[R] => M)(using sm:SpawnMethods):Future[R] = ask(message)(using sm)

}