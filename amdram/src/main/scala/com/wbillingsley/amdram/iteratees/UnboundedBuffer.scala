package com.wbillingsley.amdram.iteratees

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.collection.immutable.Queue
import java.{util => ju}


/**
  * Something that you can push to, giving  you an Enumerator
  */
class UnboundedBuffer[T](using ex:ExecutionContext) extends Enumerator[T] {
    private var queue:Queue[Input[T]] = Queue.empty
    private var senderClosed = false
    private var waitingForInput:Promise[Unit] = Promise.successful(())
    
    def close() = synchronized {
        senderClosed = true 
        queue = queue.enqueue(Input.EndOfStream) 
        if !waitingForInput.isCompleted then waitingForInput.success(())
    }

    def push(datum:T) = synchronized { 
        queue = queue.enqueue(Input.Datum(datum))
        if !waitingForInput.isCompleted then waitingForInput.success(())
     }

    def error(e:Throwable) = synchronized {
        senderClosed = true 
        queue = queue.enqueue(Input.Error(e)) 
        if !waitingForInput.isCompleted then waitingForInput.success(())
    }


    def foldOver[R](iteratee: Iteratee[T, R]): Future[R] = synchronized {
        if queue.isEmpty then 
            waitingForInput = Promise()
            waitingForInput.future.flatMap { _ => foldOver(iteratee) }
        else
            val (el, qq) = queue.dequeue
            queue = qq
            el match {
                case datum:Input.Datum[T] => iteratee.accept(datum).flatMap {
                    case RequestState.Continue(i) => foldOver(i)
                    case RequestState.Done(r) => Future.successful(r)
                    case RequestState.Error(e) => Future.failed(e)
                }
                case Input.EndOfStream => iteratee.accept(Input.EndOfStream).flatMap {
                    case RequestState.Continue(i) => Future.failed(ju.NoSuchElementException("Asked to continue at end of stream"))
                    case RequestState.Done(r) => Future.successful(r)
                    case RequestState.Error(e) => Future.failed(e)
                }
                case Input.Error(e) => iteratee.accept(Input.Error(e)).flatMap {
                    case RequestState.Continue(i) => Future.failed(ju.NoSuchElementException("Asked to continue after error"))
                    case RequestState.Done(r) => Future.successful(r)
                    case RequestState.Error(e) => Future.failed(e)
                }
            }
        
    }

}
