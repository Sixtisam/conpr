package ws

import scala.concurrent.stm._
import scala.collection.parallel.CollectionConverters._
import Util.thread
import java.util.concurrent.atomic.AtomicInteger

/* In diesem Arbeitsblatt lernen Sie ScalaSTM kennen.
 * Die notwendige Bibliothek ist bereits auf dem Klassenpfad und die STM Klassen
 * sind ebenfalls importiert: "scala.concurrent.stm._"
 * Überall wo Sie Code schreiben müssen, ist das mit ??? vermerkt.
 */


/* Zum Auffrischen betrachten wir die Klasse UnsafeAccount. Die balance ist 
 * eine ungeschützte Variable vom Typ Double.
 */
class UnsafeAccount {
  /* INV: balance >= 0 */
  private var balance: Double = 0

  def deposit(amount: Double): Unit = {
    if (amount < 0) throw new IllegalArgumentException()
    balance = balance + amount
  }

  def withdraw(amount: Double): Unit = {
    if (amount < 0) throw new IllegalArgumentException()
    if (balance - amount < 0) throw new IllegalStateException("Overdrawn!")
    balance = balance - amount
  }

  def getBalance(): Double = balance
}

/* Aufgaben:
 * 1. Welche Balance erwarten Sie nachdem eine Million mal 1CHF einbezahlt wurde? 1 Million
 * 2. Zur Repetition: Was geht schief? Nach dem Auslesen der aktuellen Balance se
 *    In einem Thread wird jeweils die Balance ausgelesen und dann um 1 erhöht wieder gesetzt.
 *    Zwischen dem auslesen und dem schreiben haben ganz viele andere Threads die Balannce bereits überschrieben.
 *    All diese Änderungen in der Zwischenzeit werden überschrieben.
 */
object _1_UnsafeTest extends App {
  val ua = new UnsafeAccount()

  println("Balance before: " + ua.getBalance())
  (1 to 1000000).par.foreach(_ => ua.deposit(1))
  println("Balance after: " + ua.getBalance())
}


/* Aufgabe: 
 * Implementieren Sie mittels STM atomic und Ref einen threadsicheren Account.
 */
class AtomicAccount {

  private val balance = Ref[Double](0.0)

  def deposit(amount: Double): Unit = atomic { implicit tx =>
        balance += amount
  }

  def withdraw(amount: Double): Unit = atomic { implicit tx =>
        if(balance.get < amount) throw new IllegalArgumentException("not enough cash")
        balance -= amount
  }

  def getBalance(): Double = atomic { implicit tx =>
    balance.get
  }
}

/* Aufgaben: 
 * 1. Notieren Sie die erwarteten Ausgaben.
 * 2. Lassen Sie das Programm laufen. Haben sich Ihre Erwartungen bestätigt?
 */
object _2_AtomicAccountTest extends App {
  val aa = new AtomicAccount()

  println("Balance before: " + aa.getBalance()) // Erwartet: 0 Korrekt
  (1 to 1000000).par.foreach(_ => aa.deposit(1))
  println("Balance after: " + aa.getBalance()) // Erwartet: 1'000'000 Korrekt
}


/* Aufgaben: 
 * 1. Untersuchen Sie die transfer Methode. Worin unterscheiden sich die 
 *    val transfers von der val attempts?
 *      Attempts sind höher, d. h. einige Transaktion mussten wiederholt werden, da in der Zwisschenzeit eine andere Transaktiong geschrieben wurde.
 * 2. Notieren Sie die erwarteten Ausgaben.
 * 3. Lassen Sie das Programm laufen. Haben sich Ihre Erwartungen bestätigt?
 */
object _3_TransferTest extends App {

  val transfers: Ref[Int] = Ref(0)
  val attempts = new AtomicInteger
  
  def transfer(a: AtomicAccount, b: AtomicAccount, amount: Double): Unit = {
    atomic { implicit tx =>
      transfers.set(transfers.get + 1)
      attempts.incrementAndGet()
      
      a.withdraw(amount)
      b.deposit(amount)  
    }
  }

  val a = new AtomicAccount()
  val b = new AtomicAccount()
  
  a.deposit(10000)
  b.deposit(10000)
  
  
  println(s"Before: a:${a.getBalance} b:${b.getBalance}")
  
  val t1 = thread { (1 to 1000).foreach{ _ => transfer(a, b, 10)} }
 // Thread.sleep(100)
  println(s"Between:  a:${a.getBalance} b:${b.getBalance}")
  val t2 = thread { (1 to 1000).foreach{ _ => transfer(b, a, 10)} }
  
  t1.join; t2.join
  
  println(s"Attempts:  ${attempts.get()}")
  println(s"Transfers: ${transfers.single()}")
  println(s"After:  a:${a.getBalance} b:${b.getBalance}")
}


/* Aufgaben:
 * 1. Untersuchen Sie den Code und notieren Sie die erwarteten Ausgaben.
 *     Ein Paar "Tx2: Read 1", dann die Transaktion 1 mit allen ausgaben und dann "Tx2: Read 12"
 * 2. Lassen Sie das Programm laufen. Haben sich Ihre Erwartungen bestätigt?
 */
object _04_Isolation extends App {
  val r = Ref(1)

  thread {
    atomic { implicit tx =>
      println("Tx1: About to change Ref")
      r.set(12)
      println("Tx1: Changed ref to " + r.get)
      Thread.sleep(3000)
      println("Tx1: About to commit")
    }
  }

  atomic { implicit tx =>
    println("Tx2: Starting Transaction")
    while (r.get != 12) {
      println("Tx2: Read " + r.get)
      Thread.sleep(400)
    }
    println("Tx2: Read " + r.get)
  }
}



object Util {
    def thread(b: => Unit): Thread = {
    val t = new Thread() {
      override def run(): Unit = b
    }
    t.start
    t
  }
}

/** Solutions */

object Solutions extends App {
  implicit class CypherString(s: String) {
    def rot13: String = s map {
      case c if 'a' <= c.toLower && c.toLower <= 'm' => c + 13 toChar
      case c if 'n' <= c.toLower && c.toLower <= 'z' => c - 13 toChar
      case c => c
    }
  }
    
  val AtomicAccount = 
"""
pynff NgbzvpNppbhag {

  cevingr iny onynapr = Ers[Qbhoyr](0)

  qrs qrcbfvg(nzbhag: Qbhoyr) = ngbzvp { vzcyvpvg gk =>
    onynapr() = onynapr() + nzbhag
  }

  qrs jvguqenj(nzbhag: Qbhoyr) = ngbzvp { vzcyvpvg gk =>
    vs (onynapr() - nzbhag < 0) guebj arj VyyrtnyFgngrRkprcgvba("Bireqenja!")
    onynapr() = onynapr() - nzbhag
  }
"""

  
  println(AtomicAccount.rot13)
}



