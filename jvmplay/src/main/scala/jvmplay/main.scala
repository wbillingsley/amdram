package jvmplay

import scala.concurrent.* 
import ExecutionContext.Implicits.global

import com.wbillingsley.amdram.*

given troupe:Troupe = SingleEcTroupe()

@main def main() = {
    println("Hello world")

    val a1 = troupe.spawnLoop[Any] { (msg) => 
        println("1: " + msg)
        //ignore    
    }

    val a2 = troupe.spawnLoop[Any] { (msg) => 
        println("2: " + msg)
        a1 ! msg
        //ignore    
    }

    val a3 = troupe.spawnLoop[Any] { (msg) => 
        println("3: " + msg)
        a2 ! msg
        //ignore    
    }

    val a4 = troupe.spawnLoop[Any] { (msg) => 
        println("4: " + msg)
        a3 ! msg
        //ignore    
    }

    val a5 = troupe.spawnLoop[Any] { (msg) => 
        println("5: " + msg)
        a4 ! msg
        //ignore    
    }

    val a6 = troupe.spawnLoop[Any] { (msg) => 
        println("6: " + msg)
        a5 ! msg
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