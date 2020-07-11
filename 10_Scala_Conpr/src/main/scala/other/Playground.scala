package other

import rx.lang.scala.Observable

import scala.collection.parallel.CollectionConverters._

object Playground extends App {

  val list = (1 to 100000).toList
  val modified = list.par
    .map {
      _ * 19 % 34
    }
    .filter {
      _ % 2 == 0
    }
    .seq.toList
  println(modified)
}

object Playground2 extends App {
  val obs = Observable.from(1 to 10000)

  obs
    .map {
      _ * 19 % 34
    }
    .filter {
      _ % 2 == 0
    }
    .subscribe(x => print("Next: " + x))
}