package docs

import com.wbillingsley.veautiful.* 
import doctacular.*
import html.*

import com.wbillingsley.amdram.*

import org.scalajs.macrotaskexecutor.MacrotaskExecutor.Implicits._

object ppGroup extends ActorGroup() with DHtmlComponent {

    val running = stateVariable(false)

    override def render = {
        // A dirty trick doing this inside the render
        if running.value then 
            workLoop()
            requestUpdate()

        <.div(
            if running.value then 
                <.button("Stop", ^.on.click --> { running.value = false })
            else 
                <.button("Start", ^.on.click --> { running.value = true })
        )
    }

}

lazy val pong = ppGroup.spawnLoop((message:String) => {
    ping ! "ping"
    println(message)
})

def pingHandler(n:Int):MessageHandler[String] = n match {
    case 0 => MessageHandler[String] { (s) =>
        val ctx = summon[ActorContext]
        pong ! "finished"
        println("ping received " + s)
        println("ping finished")
        ctx.terminate()
    }
    case n => MessageHandler[String] { (s) =>
        val ctx = summon[ActorContext]

        pong ! "ping"
        println("pong " + n)
        pingHandler(n - 1)
    }
}

lazy val ping = ppGroup.spawn(pingHandler(5))




val pingpong = <.div(
    <.article(
        """|This page will show some docs"
        """.stripMargin
    ),
    ppGroup,
    <.div(
        <.button("Send", ^.on.click --> { ping.send("Hello") })
    )


)