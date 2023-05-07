package jvmplay

import scala.concurrent.* 
import ExecutionContext.Implicits.global

import com.wbillingsley.amdram.*

given troupe:Troupe = SingleEcTroupe()

@main def main() = {
    info("hello world")

    val a1 = troupe.spawnLoop[String] { (msg) => 
        info("1: " + msg)
        //ignore    
    }

    val a2 = troupe.spawnLoop[String] { (msg) => 
        info("2: " + msg)
        a1 ! (msg + " from 2")
        //ignore    
    }

    val a3 = troupe.spawnLoop[String] { (msg) => 
        info("3: " + msg)
        a2 ! (msg + " from 3")
        //ignore    
    }

    val a4 = troupe.spawnLoop[String] { (msg) => 
        info("4: " + msg)
        a3 ! (msg + " from 4")
        //ignore    
    }

    val a5 = troupe.spawnLoop[String] { (msg) => 
        info("5: " + msg)
        a4 ! (msg + " from 5")
        //ignore    
    }

    val a6 = troupe.spawnLoop[String] { (msg) => 
        info("6: " + msg)
        a5 ! (msg + " from 6")
        //ignore    
    }

    val timer = java.util.Timer()
    val task = new java.util.TimerTask {
        def run() = 
            println("Sending!")
            a6 ! "hello"
    }
    timer.schedule(task, 16, 16)

    try {
        Thread.sleep(10000)
    } catch {
        case _ => ()
    }
}