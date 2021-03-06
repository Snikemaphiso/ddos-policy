//#full-example
package controllers

import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import models.{DdosJson, Event}
import play.api.libs.json.Json.{toJson, prettyPrint}

object EventConsumer {
  final case class ReceivedEvent(payload: Event, replyTo: ActorRef[Recipient])
  final case class Recipient(payload: Event, from: ActorRef[ReceivedEvent])

  def apply(): Behavior[ReceivedEvent] = Behaviors.receive { (context, receivedEvent) =>
    context.log.info("Received DDOS event: \n{}!", prettyPrint(toJson(receivedEvent.payload)))
    receivedEvent.replyTo ! Recipient(receivedEvent.payload, context.self)

//    context.log.info("Received DDOS event2: \n{}!", prettyPrint(toJson(receivedEvent.payload)))
//    receivedEvent.replyTo ! Recipient(receivedEvent.payload, context.self)
    Behaviors.same
  }
}

object ActionForEventBot {

  def apply(): Behavior[EventConsumer.Recipient] =
    Behaviors.receive { (context, message: EventConsumer.Recipient) =>
      context.log.info("Performing action: DENY_SOURCE for event type [{}] at Resource [{}] with Severity [{}]...", message.payload.e_type, message.payload.t_type, message.payload.severity)
      // TODO:
      //  Need to perform some action.
      //  We need to change payload from Event to Event with action,
      //  then we call `message.payload.action.performAction`
      context.log.info("Done")

      Behaviors.stopped
    }
}

object DdosPolicyMain {

  def apply(): Behavior[Event] =
    Behaviors.setup { context: ActorContext[Event] =>
      val eventConsumer = context.spawn(EventConsumer(), "NEW_EVENT_CONSUMER")

      Behaviors.receiveMessage { event: Event =>
        val replyTo: ActorRef[EventConsumer.Recipient] = context.spawn(ActionForEventBot(), "DDOS_EVENT_ACTION")

        eventConsumer ! EventConsumer.ReceivedEvent(event, replyTo)
        Behaviors.same
      }
    }
}

object DdosPolicyStart extends App with DdosJson {
  val ddosPolicyMain: ActorSystem[Event] = ActorSystem(DdosPolicyMain(), "DdosPolicyStart")

  val arguments = args.mkString
//  val arguments2 = args.mkString

  println(s"ARGS: $arguments")
//  println(s"ARGS: $arguments2")

  val events: Seq[Event] = getEventFromJsonString(arguments)
//  val events2: Seq[Event] = getEventFromJsonString(arguments2)

  if (events.isEmpty) {
    println("No passed args or args are not in the expected format")

  } else {
    for (policyEvent: Event <- events) yield ddosPolicyMain ! policyEvent
  }

}