package assignment

import java.util.concurrent.{CompletableFuture, Executors, Future}

import scala.annotation.tailrec

// Assigment 9

object Task1 {
  val exSvc = Executors.newCachedThreadPool()

  @tailrec
  def seqMapHelper[A,B](l: List[A], f: A => B, accumulator : List[B]): List[B] = {
    l match {
      case Nil => accumulator
      case x :: xs => seqMapHelper(xs, f, accumulator :+ f(x))
    }
  }

  def seqMap[A, B](l: List[A], f: A => B): List[B] = {
    seqMapHelper(l, f, List())
  }

  def parMap[A, B](list: List[A], f: A => B): List[B] = {
    // Map to Future[B]
    val mapFunc = (el: A) => exSvc.submit(() => f(el));
    // Submit Callable for each element, in the end wait for completion
    list.map(mapFunc)
        .map(_.get())
  }

  def main(args: Array[String]): Unit = {
    println(parMap(List(1, 2, 3, 4, 5, 6, 7, 8, 9), (x: Int) => x + 2))
    exSvc.shutdownNow()
  }
}


object Task2 {
  val exSvc = Executors.newCachedThreadPool()

  @tailrec
  def seqFilterHelper[A](l: List[A], p: A => Boolean, accumulator : List[A]): List[A] = {
    l match {
      case Nil => accumulator
      case x :: xs => seqFilterHelper(xs, p, if(p(x)) accumulator :+ x else accumulator)
    }
  }

  def seqFilter[A](l: List[A], p: A => Boolean): List[A] = {
    seqFilterHelper(l, p, List())
  }

  def parFilter[A](l: List[A], p: A => Boolean): List[A] = {
    // Schedule predicate in executor service, map to (A, Future[Boolean])
    val mapFunc = (el: A) => (el, exSvc.submit(() => p(el)))
    val tupleList = l.map(mapFunc)
    // Wait for all Future[Boolean] and filter by it
    tupleList
      .filter(_._2.get)
      .map(_._1)
  }

  def main(args: Array[String]): Unit = {
    println(seqFilter((1 to 100).toList, (x: Int) => x > 23))
  }
}

object Task3 {
  // Assuming left reduce
  @tailrec
  def seqReduceTail[A](l: List[A], f: (A, A) => A, accumulator : A): A = {
    if (l.isEmpty)
      throw new IllegalArgumentException("list must not be empty")
    else l match {
      case x :: Nil => f(x, accumulator)
      case x :: xs => seqReduceTail(xs, f, f(x, accumulator))
    }
  }

  val exSvc = Executors.newCachedThreadPool()

  /**
   * reduces the array pairwaise [a,b,c,d] => [Future[f(a,b)], Future[f(c,d)]]
   */
  @tailrec
  def parReducePairwise[A](l: List[A], f: (A, A) => A, accumulator : List[Future[A]]): List[Future[A]] = {
    l match {
      case Nil => accumulator
      case x :: Nil => accumulator :+ CompletableFuture.completedFuture(x)
      case x :: y :: xs => parReducePairwise(xs, f, accumulator :+ exSvc.submit(() => f(x, y)))
    }
  }

  // f must be commutative in order to be logically equivalent to seqReduce
  @tailrec
  def parReduce[A](fullList: List[A], reduceFunc: (A, A) => A): A = {
    if (fullList.isEmpty)
      throw new IllegalArgumentException("list must not be empty")
    else {
      // Reduce pairwise and wait for future completion on each
      val futureList = parReducePairwise(fullList, reduceFunc, List()).map(_.get)
      futureList match {
        // if only two values left, apply reduceFunc to it and complete
        case x :: y :: Nil => reduceFunc(x, y)
        // if more than 2 values left, call parReduce again with that list
        case _ => parReduce(futureList, reduceFunc)
      }
    }
  }

  def main(args: Array[String]): Unit = {
    // Big lists dont work because of stackoverflow errors
    println(seqReduceTail((1l to 1000000l).toList, (a: Long, b: Long) => a + b, 0l))
    exSvc.shutdownNow()
  }
}