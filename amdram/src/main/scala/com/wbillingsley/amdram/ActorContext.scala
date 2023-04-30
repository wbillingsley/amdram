package com.wbillingsley.amdram

/**
  * Passed into an Actor when it is handling a message
  */
trait ActorContext[T] extends SpawnMethods {

    def terminate():Unit

    def self:Recipient[T]

}
