package com.wbillingsley.amdram

/**
  * Passed into an Actor when it is handling a message
  */
trait ActorContext[T] {

    def spawnLoop[T](f: T => ActorContext[T] ?=> Unit):Recipient[T]

    def spawn[T](handler:MessageHandler[T]):Recipient[T]

    def terminate():Unit

    def self:Recipient[T]

}
