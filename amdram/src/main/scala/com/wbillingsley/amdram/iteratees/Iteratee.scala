package com.wbillingsley.amdram.iteratees

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.collection.immutable.Queue
import java.{util => ju}


/**
 * An item in a stream
 */
enum Input[+T]:
    case Datum(datum:T)
    case EndOfStream
    case Error(e:Throwable)

/**
 * An iteratee receives a stream.
 * The stream can push an item to it, and when the iteratee has finished processing it, it can say that it is ready for another item
 */    
enum RequestState[-T, +R]:
    case Done(result:R)
    case Error(e:Throwable)
    case Continue(i:Iteratee[T, R])

/**
 * Receives a stream of data asynchronously.
 */
trait Iteratee[-T, +R] {
    def accept(datum:Input[T]): Future[RequestState[T, R]]
}

/**
 * An asynchronous stream of data
 */
trait Enumerator[+T] {

    /**
     * Pushes data to the iteratee. I've called this "foldOver", because of its similarity to fold on lists:  
     *
     * List(1,2,3).foldLeft(state)((state, num) => state) 
     * accepts a function that will be called for each element of the list
     * 
     * Enumerator.fromIterable(List(1,2,3)).foldOver(iteratee)
     * accepts an "iteratee" that will effectively be called for each element of the list. 
     * The iteratee encapsulates both the state and the function to apply to reach the next state. 
     * Note however that an iteratee could return a different iteratee to apply to the next element of the stream.
     */
    def foldOver[R](iteratee:Iteratee[T, R]): Future[R]
}

type Transformer[A, B] = Input[A] => Option[Input[B]] 

class AdaptedIteratee[A, B, R](iteratee:Iteratee[B, R])(transformer:Transformer[A, B])(using ec:ExecutionContext) extends Iteratee[A, R] {
    def accept(datum:Input[A]):Future[RequestState[A, R]] = {
        transformer(datum) match {
            case Some(datum) => iteratee.accept(datum).map {
                case RequestState.Done(r) => RequestState.Done(r)
                case RequestState.Error(e) => RequestState.Error(e)
                case RequestState.Continue(i) => RequestState.Continue(new AdaptedIteratee(i)(transformer))
            }
            case None => Future.successful(RequestState.Continue(this))
        }
    }
}


/**
 * Turns any IterableOnce into an Enumerator
 */
class IterableEnumerator[T](iterable:IterableOnce[T])(using ec:ExecutionContext) extends Enumerator[T] {
 
    def foldOver[R](iteratee:Iteratee[T, R]): Future[R] = {
        val it = iterable.iterator
        def loop(i:Iteratee[T, R]):Future[R] = {
            if it.hasNext then
                i.accept(Input.Datum(it.next)).flatMap {
                    case RequestState.Continue(i) => loop(i)
                    case RequestState.Done(r) => Future.successful(r)
                    case RequestState.Error(e) => Future.failed(e)
                }
            else
                i.accept(Input.EndOfStream).flatMap {
                    case RequestState.Continue(i) => Future.failed(ju.NoSuchElementException("Asked to continue at end of stream"))
                    case RequestState.Done(r) => Future.successful(r)
                    case RequestState.Error(e) => Future.failed(e)
                }
        }
        loop(iteratee)
    }
}


object Enumerator {
    def fromIterable[T](iterable:IterableOnce[T])(using ec:ExecutionContext) = IterableEnumerator(iterable)
}


extension [T] (e:Enumerator[T]) {

    def map[B](f: T => B)(using ec:ExecutionContext):Enumerator[B] = new Enumerator[B] {
        def foldOver[R](iteratee:Iteratee[B, R]): Future[R] = {
            e.foldOver(new AdaptedIteratee(iteratee)({
                    case Input.Datum(datum) => Some(Input.Datum(f(datum)))
                    case Input.EndOfStream => Some(Input.EndOfStream)
                    case Input.Error(e) => Some(Input.Error(e))
            }))
        }
    }

    def foreach(f: T => Unit)(using ec:ExecutionContext) = e.map(f) 

    def filter(f: T => Boolean)(using ec:ExecutionContext):Enumerator[T] = new Enumerator[T] {
        def foldOver[R](iteratee:Iteratee[T, R]): Future[R] = {
            e.foldOver(new AdaptedIteratee(iteratee)({
                    case Input.Datum(datum) if f(datum) => Some(Input.Datum(datum))
                    case Input.Datum(_) => None
                    case Input.EndOfStream => Some(Input.EndOfStream)
                    case Input.Error(e) => Some(Input.Error(e))
            }))
        }
    }


}