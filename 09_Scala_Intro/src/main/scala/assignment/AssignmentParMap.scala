package assignment

import java.util.concurrent.{CompletableFuture, Executors, Future}

// Assigment 9

object Task1 {
  val exSvc = Executors.newCachedThreadPool()

  def seqMap[A, B](l: List[A], f: A => B): List[B] = {
    l match {
      case Nil => Nil
      case x :: xs => f(x) :: seqMap(xs, f)
    }
  }

  def parMap[A, B](l: List[A], f: A => B): List[B] = {
    // Map to Future[B]
    val mapFunc = (el: A) => exSvc.submit(() => f(el));
    val futureList = l.map(mapFunc)
    // Wait for all Future[B] and map back to B
    futureList.map(_.get())
  }

  def main(args: Array[String]): Unit = {
    println(parMap(List(1, 2, 3, 4, 5, 6, 7, 8, 9), (x: Int) => x + 2))
    exSvc.shutdownNow()
  }
}


object Task2 {
  val exSvc = Executors.newCachedThreadPool()
  def seqFilter[A](l: List[A], p: A => Boolean): List[A] = {
    l match {
      case Nil => Nil
      case x :: xs => if (p(x)) x :: seqFilter(xs, p) else seqFilter(xs, p)
    }
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
    println(parFilter(List(1, 2, 3, 4, 5, 6), (x: Int) => x > 2))
  }
}

object Task3 {
  // Assuming left reduce
  def seqReduce[A](l: List[A], f: (A, A) => A): A = {
    if (l.isEmpty)
      throw new IllegalArgumentException("list must not be empty")
    else l match {
      case x :: y :: Nil => f(x, y)
      case x :: xs => f(x, seqReduce(xs, f))
    }

  }

  val exSvc = Executors.newCachedThreadPool()

  /**
   * reduces the array pairwaise [a,b,c,d] => [Future[f(a,b)], Future[f(c,d)]]
   */
  def parReducePairwise[A](l : List[A], f: (A,A) => A) : List[Future[A]] = {
    l match {
      case Nil => List()
      case x :: Nil => List(CompletableFuture.completedFuture(x))
      case x :: y :: xs => exSvc.submit(() => f(x,y)) :: parReducePairwise(xs, f)
    }
  }

  // f must be commutative in order to be logically equivalent to seqReduce
  def parReduce[A](fullList: List[A], reduceFunc: (A, A) => A): A = {

    if (fullList.isEmpty)
      throw new IllegalArgumentException("list must not be empty")
    else {
      // Reduce pairwise and wait for future completion on each
      val futureList = parReducePairwise(fullList, reduceFunc).map(_.get)
      futureList match {
          // if only two values left, apply reduceFunc to it and complete
        case x :: y :: Nil => reduceFunc(x,y)
          // if more than 2 values left, call parReduce again with that list
        case _ => parReduce(futureList, reduceFunc)
      }
    }
  }

  def main(args: Array[String]): Unit = {
    // Big lists dont work because of stackoverflow errors
    println(parReduce((1 to 100).toList, (a: Int, b: Int) => a + b))
  }
}