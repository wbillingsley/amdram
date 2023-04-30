package docs

import com.wbillingsley.veautiful.* 
import html.*

val home = <.div(
    marked.div(
        """|# Amdram
           |
           |Amdram is a very small Scala library for Actors. The name is a common nickname for amateur dramatics, hopefully conveying its lightweight intentions.
           |It's intended to be small enough to be understandable by students.
           |
           |It's cross-published for Scala 3 on the JVM and Scala.js
           |
           |```scala
           |library-dependencies += "com.wbillingsley" %%% "amdram" % version
           |```
           |
           |At the time of writing, snapshot versions are being published to Sonatype Snapshots (oss.sonatype.org)
           |
           |## Basic concepts
           |
           |This tries quickly to run through the core classes in Amdram actors
           |
           |### Recipients
           |
           |A `Recipient[-T]` is something that can receive messages, `T`
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
           |The `send` method is aliased to `!` for Erlang-like syntax, and also to tell for rough similarity with syntax students might see in Akka
           |
           |Note that `ask` becomes available as an extension method. See later on that.
           |
           |### Troupe
           |
           |A system of actors, in Amdram, is punningly called a Troupe.
           |A Troupe is responsible for spawning new actors.
           |
           |By using different kinds of troupe, we can run systems of actors in different situations: e.g. running on lots of virtual threads using Project Loom,
           |or all running in a microtask evaluator in single-threaded Scala.js
           |
           |In the initial version, a troupe's interface is simply defined as
           |
           |```scala
           |trait Troupe {
           |
           |    /** Spawns an actor that will always use the same handling function */
           |    def spawnLoop[T](f: T => ActorContext[T] ?=> Unit):Recipient[T]
           |
           |    /** Spawns a handler that might work like a finite state machine, producing a different handler after each invocation */
           |    def spawn[T](handler:MessageHandler[T]):Recipient[T]
           |
           |}
           |```
           |
           |You'll notice there are *two* methods for spawning new actors. This is trying to ensure we can handle things like the ping-pong example.
           |
           |In this example [from the Erlang docs](https://www.erlang.org/doc/getting_started/conc_prog.html), note that 
           |`pong()` always does the same thing, but `ping()` replaces itself with a `ping(N - 1, Pong_PID)` at the end.
           |
           |```erlang
           |-module(tut15).
           |
           |-export([start/0, ping/2, pong/0]).
           |
           |ping(0, Pong_PID) ->
           |    Pong_PID ! finished,
           |    io:format("ping finished~n", []);
           |
           |ping(N, Pong_PID) ->
           |    Pong_PID ! {ping, self()},
           |    receive
           |        pong ->
           |            io:format("Ping received pong~n", [])
           |    end,
           |    ping(N - 1, Pong_PID).
           |
           |pong() ->
           |    receive
           |        finished ->
           |            io:format("Pong finished~n", []);
           |        {ping, Ping_PID} ->
           |            io:format("Pong received ping~n", []),
           |            Ping_PID ! pong,
           |            pong()
           |    end.
           |
           |start() ->
           |    Pong_PID = spawn(tut15, pong, []),
           |    spawn(tut15, ping, [3, Pong_PID]).
           |```
           |
           |To allow this state machine like behaviour, we have a `MessageHandler[T]` type.
           |
           |### Loops and ActorContext
           |
           |The `Pong` handler, being constant, can just be launched like this:
           |
           |```scala
           |lazy val pong = pingPongTroupe.spawnLoop((message:String) => {
           |    ping ! "ping"
           |    println(message)
           |})
           |```
           |
           |However, you might want to call some kind of function on the `ActorContext` (e.g. to launch a new actor, or terminate the actor).
           |In this case, an `ActorContext` is supplied as an implicit parameter, and can be accessed using `summon`
           |
           |```scala
           |But the `Pong` handler, being constant, can just be launched like this:
           |
           |```scala
           |lazy val pong = pingPongTroupe.spawnLoop((message:String) => {
           |    val context = summon[ActorContext[_]]
           |    // Now I could call methods on context
           |
           |    ping ! "ping"
           |    println(message)
           |})
           |```
           |
           |### MessageHandler
           |
           |In the ping example, the actor behaves like a finite state machine, changing its behaviour after each message is received. 
           |A `MessageHandler[T]` lets us define such a thing. It's defined as
           |
           |```scala
           |trait MessageHandler[T] {
           |    def receive(message:T)(using ac: ActorContext[T]): MessageHandler[T] | Unit
           |}
           |```
           |
           |Just as with Erlang's ping-pong example above, we can either just return `Unit` if the same behaviour should
           |be used again for the following message, or we can return a replacement `MessageHandler[T]` if we want the 
           |actor to use that for the next message instead.
           |
           |To make them easier to create, there are two methods in the companion object, depending on whether you want to receive the `ActorContext`
           |as a second parameter to your function, or receive it implicitly.
           |
           |```scala
           |object MessageHandler {
           |    // Takes a function that has a single parameter, but also receives the ActorContext as an implicit parameter.
           |    // The ActorContext can be retrieved with summon[ActorContext[_]]
           |    def apply[T](f: T => ActorContext[T] ?=> MessageHandler[T] | Unit):MessageHandler[T] = // implementation omitted
           |
           |    // Takes a function that receives the message as its first parameter and the ActorContext as its second
           |    def apply[T](f: (T, ActorContext[T]) => MessageHandler[T] | Unit):MessageHandler[T] = // implementation omitted
           |}
           |```
           |
           |
           |So, using Amdram, launching the `Ping` actor might look like this:
           |
           |```scala
           |def pingHandler(n:Int, pong:Recipient[String]):MessageHandler[String] = n match {
           |    case 0 => MessageHandler { (s, ctx) =>
           |        pong ! "finished"
           |        println("ping received " + s)
           |        println("ping finished")
           |        ctx.terminate()
           |    }
           |    case n => MessageHandler[String] { (s) =>
           |        pong ! "ping"
           |        println("pong " + n)
           |        pingHandler(n - 1, pong)
           |    }
           |}
           |
           |lazy val ping:Recipient[String] = pingPongTroupe.spawn(pingHandler(5, pong))
           |```
           |
           |
           |### Available Troupes
           |
           |At the moment, there is a single kind of `Troupe` defined in the library: `SingleEcTroupe`, which starts all its actors on the same 
           |execution context. This is beause it was written to run in Scala.js's single-threaded environment, using the microtask execution context.
           |
           |```scala
           |// or use ExecutionContext.Implicits.global if you're not importing the microtask evaluator
           |import org.scalajs.macrotaskexecutor.MacrotaskExecutor.Implicits._
           |val pingPongGroup = SingleEcGroup()
           |```
           |
           |Note that if you're using this, then an infinite loop in one actor would keep the execution task fully occupied, stalling your program. 
           |(JavaScript is single-threaded, so in Scala.js we are often writing an actor system that in fact all runs inside a single thread.)
           |
           |### Actor
           |
           |So far, we've talked about launching Actors without mentioning the `Actor` class/trait itself. It does exist. They can be independently launched.
           |
           |`Actor` is a trait with a single undefined member:
           |
           |```scala
           |    def receive(message:T):ActorContext[T] ?=> Unit
           |```
           |
           |It can be started in a troupe on an execution context with
           |
           |```scala
           |actor.start()(using Actor.context(actor, troupe), executionContext)
           |```
           |
           |It's not really recommended to do that though, as the `Troupe` is a nice place to define how execution contexts should be set up, whereas this
           |supplies the execution context of this actor separately from the Troupe.
           |
           |Instead, if you want to implement your actor as an object or class extending a `Trait`, just extend `MessageHandler[T]` and spawn it.
           |
           |### The ask pattern
           |
           |Sometimes, an actor might want to ask another actor a question, getting the reply in a `Future[R]`
           |
           |Typically, the problem is knowing what type of reply will be received. In the initial version of Amdram, an `ask` pattern is implemented using
           |an inline extension method. That is, the code will be inlined (by Scala 3) into your actor, rather than called as a method.
           |
           |```
           |val reply = destination.ask[Reply](message) // Future[Reply]
           |```
           |
           |It works by spawning a temporary actor whose only job is to wait to receive the reply.
           |
           |The code of the inlined method is:
           |
           |```scala
           |extension [M] (a:Recipient[M]) {
           |
           |    inline def ask[T, R](message:M)(using ag:ActorContext[T]):Future[R] = {
           |        val p = Promise[R]
           |        ag.spawnLoop[R] { reply =>
           |            reply match {
           |                case r:R => p.success(reply)
           |                case other:Any => p.failure(IllegalArgumentException(s"Unexpected reply $other"))
           |            }
           |            ag.terminate()
           |        }
           |        a.send(message)
           |        p.future
           |    }
           |
           |    inline def ?[T, R](message:M)(using ag:ActorContext[T]):Future[R] = ask(message)(using ag)
           |
           |}
           |```
           |
           |
           |""".stripMargin

    )
)