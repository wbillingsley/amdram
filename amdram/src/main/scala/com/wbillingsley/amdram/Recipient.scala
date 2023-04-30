package com.wbillingsley.amdram

/** Something you can send messages to */
trait Recipient[-T] {
    def send(message:T):Unit

    /** alias for send */
    def tell(message:T):Unit = send(message)

    /** alias for send */
    def !(message:T):Unit = send(message)
}