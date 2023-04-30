package com.wbillingsley.amdram

/**
  * Fundamental to Actors in Erlang is what to do when you receive a message.
  * From the ping-pong example in the Erlang docs, we also need to enable the
  * case where an Actor changes to a different message handler at the end -
  * so we allow a MessageHandler either to return Unit (re-use the same behaviour)
  * or another MessageHandler
  */
trait MessageHandler[T] {
    def receive(message:T)(using ac: ActorContext[T]): MessageHandler[T] | Unit
}

object MessageHandler {
    def apply[T](f: T => ActorContext[T] ?=> MessageHandler[T] | Unit):MessageHandler[T] = {
        new MessageHandler[T] {
            override def receive(message:T)(using ac:ActorContext[T]) = 
                f.apply(message).apply
        }     
    }

    def apply[T](f: (T, ActorContext[T]) => MessageHandler[T] | Unit):MessageHandler[T] = 
        apply[T] { (msg:T) => (ac:ActorContext[T]) ?=> f(msg, ac) }
}