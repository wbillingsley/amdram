package docs

import com.wbillingsley.veautiful.* 
import doctacular.*
import html.*

import com.wbillingsley.amdram.*

import org.scalajs.macrotaskexecutor.MacrotaskExecutor.Implicits._
import scala.concurrent.*

object pingPongTroupe extends SingleEcTroupe() with DHtmlComponent {

    val running = stateVariable(false)

    override def render = {
        // A dirty trick doing this inside the render
        if running.value then 
            requestUpdate()

        <.div(
            if running.value then 
                <.button("Stop", ^.on.click --> { running.value = false })
            else 
                <.button("Start", ^.on.click --> { running.value = true })
        )
    }

}

lazy val pong = pingPongTroupe.spawnLoop((message:String) => {
    ping ! "ping"
    println(message)
})

def pingHandler(n:Int, pong:Recipient[String]):MessageHandler[String] = n match {
    case 0 => MessageHandler { (s, ctx) =>
        pong ! "finished"
        println("ping received " + s)
        println("ping finished")
        ctx.terminate()
    }
    case n => MessageHandler[String] { (s) =>
        pong ! "ping"
        println("pong " + n)
        pingHandler(n - 1, pong)
    }
}

lazy val ping:Recipient[String] = pingPongTroupe.spawn(pingHandler(5, pong))

lazy val echo = pingPongTroupe.spawnLoop[(String, Recipient[String])] { (message, sender) => 
    sender ! s"Echo $message"
    println(message)
}

val pingpong = <.div(
    marked.div(
        """|## Ping pong demo
           |
           |This is just a testing demo, which happens inside the console. It'll hopefully be turned into something more 
           |useful in a future update.
        """.stripMargin
    ),
    pingPongTroupe,
    <.div(
        <.button("Send", ^.on.click --> { ping.send("Hello") })
    ),
    <.div(
        <.button("Echo", ^.on.click --> { 
            given SpawnMethods = pingPongTroupe

            val f:Future[String] = echo.ask((r) => ("Hello", r))
            f.foreach(m => println(s"Completed with $m")) 
        })
    )


)