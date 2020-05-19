package ws
import scala.concurrent.stm._
import Util.thread
import java.util.concurrent.atomic.AtomicInteger

/* Aufgaben:
 * 1. Notieren Sie die erwarteten Ausgaben.
 *    a 1 0
 *    b 13 13
 *    c 13 13
 *    d 26 26
 *    a 1 26
 *    b 23 13
 *    c 13 13
 *    d 26 26
 *    e 13 26
 *    f 13 26
 *    g 13 26
 * 2. Lassen Sie das Programm laufen. Haben sich Ihre Erwartungen bestätigt?
 */
object _05_AtomicityExceptions extends App {
  val r = Ref(1)
  var nonManaged:Int = 0
  
  atomic { implicit tx =>

    try {
      println(s"a:  ${r()} $nonManaged")
      r() = 13
      nonManaged = 13
      println(s"b: ${r()} $nonManaged")

      atomic { implicit tx =>
        println(s"c: ${r()} $nonManaged")
        r() = 26
        nonManaged = 26
        println(s"d: ${r()} $nonManaged")
        throw new IllegalStateException()
      }

    } catch {
      case ex: IllegalStateException => 
        println(s"e: ${r()} $nonManaged")
    }

    println(s"f: ${r()} $nonManaged")
  }
  
  println("--------")
  println(s"g: ${r.single()} $nonManaged")
}

/* Aufgaben:
 * 1. Notieren Sie die erwarteten Ausgaben. Wann wird das "After commit" ausgegeben.
 *   start outer tx
 *   start inner tx
 *   end inner tx
 *   end outer tx
 *   after commit 10
 *   done
 * 2. Lassen Sie das Programm laufen. Haben sich Ihre Erwartungen bestätigt?
 */
object _06_AfterCommit extends App {

  val a = Ref(10)

  atomic { implicit tx =>
    println("Start outer tx")

    atomic { implicit tx =>
      println("Start inner tx")
      Txn.afterCommit(_ => println("After commit" +a.single.get))
      println("End inner tx")
    }

    println("End outer tx")
  }

  println("Done")
}
