package com.wbillingsley.amdram

import scala.concurrent.*

/**
  * The methods that spawning contexts support
  */
trait SpawnMethods {

    /** Spawns an actor that will always use the same handling function */
    def spawnLoop[T](f: T => ActorContext[T] ?=> Unit):Recipient[T]

    /** Spawns a handler that might work like a finite state machine, producing a different handler after each invocation */
    def spawn[T](handler:MessageHandler[T]):Recipient[T]

}

/**
  * A system of actors, which we've punningly called a Troupe.
  *
  * @param ec
  */
trait Troupe extends SpawnMethods


/**
  * A troupe that uses a single execution context for all of its actors
  *
  * @param ec
  */
class SingleEcTroupe(using ec:ExecutionContext) extends Troupe {

    private var actors:Set[Actor[_]] = Set.empty

    def spawnLoop[T](f: T => ActorContext[T] ?=> Unit):Recipient[T] = 
      val a = new Actor[T] {
        override val inbox = new Inbox[T](_ => this.schedule()(using Actor.context(this, SingleEcTroupe.this), ec))

        def receive(msg:T) = 
          f(msg)
      }


      enlist(a)
      a.start()(using Actor.context(a, this))
      a.self

    def spawn[T](handler:MessageHandler[T]):Recipient[T] = 
      val a = new Actor[T] {
        override val inbox = new Inbox[T](_ => this.schedule()(using Actor.context(this, SingleEcTroupe.this), ec))

        var handler:MessageHandler[T] = handler
        
        override def receive(msg:T) = (ac:ActorContext[T]) ?=>
          this.handler = handler.receive(msg) match {
            case _:Unit => this.handler
            case mh:MessageHandler[T] @unchecked => mh
          }
      }
      enlist(a)
      a.start()(using Actor.context(a, this))
      a.self


    def enlist[T](actor:Actor[T]):Unit = 
        synchronized {
            actors = actors + actor
        }

    def delist[T](actor:Actor[T]):Unit = 
        synchronized {
            actors = actors - actor
        }

}
