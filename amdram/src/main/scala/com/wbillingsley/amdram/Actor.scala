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

    def busy:Boolean  = synchronized { currentMessage.nonEmpty }

    def stop():Unit = synchronized { alive = false }

    def start()(using ac:ActorContext[T], ec:ExecutionContext):Unit = synchronized { 
        alive = true
        schedule()
    }

    def schedule()(using ac:ActorContext[T], ec:ExecutionContext):Unit = synchronized {
        if !scheduled then
            // println(s"$_id woke up")
            scheduled = true
            ec.execute(() => workLoop())
            // println(s"$_id scheduled")
    }

    /** Takes a task from the queue and performs it. */
    private def workLoop()(using context:ActorContext[T], ec:ExecutionContext):Unit = {
        // println(s"$_id entered workloop")
        currentMessage = synchronized {
            scheduled = false
            inbox.pop()
        }

        currentMessage match {
            case Some(m) => 
                // println(s"$_id processing $m")
                receive(m)
            case None => 
                // println(s"$_id's inbox was empty")
        }

        // TODO: Make this only schedule work if there's more to do (but that requires our inbox to wake us up if new work comes in)
        if alive then
            if inbox.isEmpty then
                ()
                // TODO: add low-level logging // println(s"$_id went to sleep") 
            else schedule()
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