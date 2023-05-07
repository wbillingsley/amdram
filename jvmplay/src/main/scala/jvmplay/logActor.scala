package jvmplay

enum LogLevel:
    case Trace
    case Debug
    case Info
    case Warning
    case Error

def trace(s:String) = if LogLevel.Trace.ordinal >= printLevel.ordinal then logActor ! (LogLevel.Trace -> s)
def debug(s:String) = if LogLevel.Debug.ordinal >= printLevel.ordinal then logActor ! (LogLevel.Debug -> s)
def info(s:String) = if LogLevel.Info.ordinal >= printLevel.ordinal then logActor ! (LogLevel.Info -> s)
def warning(s:String) = if LogLevel.Warning.ordinal >= printLevel.ordinal then logActor ! (LogLevel.Warning -> s)
def error(s:String) = if LogLevel.Error.ordinal >= printLevel.ordinal then logActor ! (LogLevel.Error -> s)

val printLevel = LogLevel.Info

/**
  * The log actor just prints everything to the terminal
  */
val logActor = troupe.spawnLoop[(LogLevel, String)] { (level, message) => 
    if level.ordinal >= printLevel.ordinal then println(s"$level $message")
}