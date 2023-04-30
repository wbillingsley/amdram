package docs

package docs

import com.wbillingsley.veautiful.* 
import html.*

val home = <.div(
    marked.div(
        """|# Actors - basic concepts
           |
           |This tries quickly to run through the basic classes in Amdram, while roughly explaining messages
           |
           |### Actors as a concept
           |
           |An Actor has an inbox, which fills up with messages as other actors (and other sources) send them to it.
           |
           |An Actor takes one message at a time from the inbox, and processes it. It might change local state, ask to spawn other actors, or send messages
           |to other actors.
           |
           |So, an actor's work loop is to take a message from the inbox and handle it.
           |
           |For more, there's quite a lot of material on the Actor model about. Let's move on to how Amdram implements this.
           |
           |### Recipients
           |
           |A `Recipient[-T]` is something that can receive messages, `T`. It's defined as
           |
           |```scala
           |trait Recipient[-T] {
           |    def send(message:T):Unit           
           |
           |    def tell(message:T):Unit = send(message)
           |
           |    def !(message:T):Unit = send(message)
           |}
           |```
           |
           |The `send` method is aliased to `!` for Erlang-like syntax, and also to tell for rough similarity with syntax students might see in Akka.
           |
           |An actor's inbox is a possible recipient of messages.
           |
           |
           |### ActorGroup
           |
           |An actor group (a troupe?) has a bunch of actors. It also has an `ExecutionContext` it runs on, to schedule those actors' work loops.
           |This lets us, for instance, define an ActorGroup that'll work using Project Loom green threads, or using Scala.js's microtask scheduler.
           |
           |```scala
           |import org.scalajs.macrotaskexecutor.MacrotaskExecutor.Implicits._
           |val myGroup = ActorGroup()
           |```
           |
           |The workloop of the `ActorGroup` is not automatically called. This is so that in demonstrations, we can do things like schedule it on an animation timer
           |so that it works slowly enough to be visible.
           |
           |
           |
           |
           |
           |
           |
           |
           |
           |
           |
           |
           |""".stripMargin

    )
)