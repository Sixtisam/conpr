package chat

import java.util

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.duration._
import scala.jdk.CollectionConverters._
import scala.util.Success

/**
 * Design Considerations:
 * Each described Entity is an actor, that means
 *  - User
 *  - ChatRoom
 *  - ChatServer
 */

object ChatApp {

  /**
   * All the actions between the actors
   */

  case class Msg(msg: String, sender: ActorRef)

  case class CreateRoom(name: String)

  case object ListRooms

  case object Join

  case object Leave

  class ChatRoom() extends Actor {
    val members = new util.HashSet[ActorRef]()

    override def receive: Receive = {
      case Join => members.add(sender)
      case Leave => members.remove(sender)
      case msg: Msg => members.forEach(m => m ! msg)
      case _: Any => println("ignore")
    }
  }

  class ChatServer extends Actor {
    val chatrooms = new util.HashMap[String, ActorRef]()

    override def receive: Receive = {
      case CreateRoom(name) => {
        val room = context.actorOf(Props[ChatRoom], name)
        chatrooms.put(name, room)
        sender ! room
        println("Created room " + name)
      }
      case ListRooms => sender ! chatrooms.asScala.toMap
    }
  }

  def main(args: Array[String]): Unit = {
    val as = ActorSystem("ChatApp")
    import as.dispatcher
    implicit val timeout = Timeout(3.seconds)

    val chatServer = as.actorOf(Props[ChatServer])

    val user1 = as.actorOf(Props(new Actor {
      // Create & Join Room
      val roomFuture = chatServer ? CreateRoom("conpr-room")
      roomFuture
        .onComplete({
          case Success(createdRoom: ActorRef) => createdRoom ! Join
          case _: Any => println("Failed to create room")
        })

      override def receive: Receive = {
        case Msg(msg, from) => println("User1 received '" + msg + "' from '" + from + "'")
      }
    }))


    val user2 = as.actorOf(Props(new Actor {
      // Join Room Named "conpr-room"
      val listFuture = chatServer ? ListRooms
      listFuture
        .mapTo[Map[String, ActorRef]]
        .map(rooms => rooms.get("conpr-room").orElse(null)) // get desired room
        .onComplete({
          case Success(Some(room: ActorRef)) => {
            room ! Join
          }
          case _: Any => println("Failed to create & join room")
        })

      override def receive: Receive = {
        case Msg(msg, from) => println("User2 received '" + msg + "' from '" + from + "'")
      }
    }))

    val user3 = as.actorOf(Props(new Actor {
      // Join Room Named "conpr-room"
      val listFuture = chatServer ? ListRooms
      listFuture
        .mapTo[Map[String, ActorRef]]
        .map(rooms => rooms.get("conpr-room").orElse(null))
        .onComplete({
          case Success(Some(room: ActorRef)) => {
            room ! Join
            room ! Msg("Hello World", self) // i know, sender could be used
          }
          case _: Any => println("Failed to create and join room")
        })

      override def receive: Receive = {
        case Msg(msg, from) => println("User3 received '" + msg + "' from '" + from + "'")
      }
    }))
  }
}
