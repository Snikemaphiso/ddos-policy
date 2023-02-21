package controllers

import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import models.{Event, Policy, SprayJsonImplicits}
import service.{PolicyEngine}

import scala.concurrent.Future

object Router extends SprayJsonImplicits with Cache {

  val host: String = "localhost"
  val port: Int = 8080

  val route: Route = {
    concat(
      path("event") {
        get {
          complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "<h1>Call this endpoint with a valid event JSON as a payload, using POST  </h1>"))
        }
      },
      path("event") {
        post {
          entity(as[Event]) { event: Event =>
            println(event)
            complete(HttpEntity(ContentTypes.`application/json`, s"<h1>Event of type [${event.event_type.attack_class}] receive successfully</h1>"))
          }
        }
      },
      path("policy") {
        get {
          complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "<h1>Call this endpoint with a valid policy JSON as a payload, using POST </h1>"))
        }
      },
      path("policy") {
        post {
          entity(as[Policy]) { policy: Policy =>
            policyCache.put(policy.name, Future.successful(policy))
            complete(HttpEntity(ContentTypes.`application/json`, s"<h1>Policy [${policy.name}] received successfully</h1>"))
          }
        }
      },
      path("policy" / "list") {
        get {
          println(printAllPoliciesInCache)
          complete(
            HttpEntity(
              ContentTypes.`text/html(UTF-8)`,
              s"""<h2>Policies in cache are:</h2>
                  <p>$getAllCachedPolicies</p>
              """
            ))
        }
      },

      path("trigger") {
        post {
          println("-----Trigger match engine.")
          entity(as[Event]) { event: Event =>
            getAllCachedPolicyObjects.find {
              PolicyEngine.isEventMatching(_, event)
            } match {
              case Some(p) => {
                println(s"Policy ${p} matched with event ${event}")
                PolicyEngine.getActionObject(p) match {
                  case Some(pa) => pa.performPAction(event)
                  case _ => println("No Policy Action Found.")
                }
              }
              case _ => println("No Policy Matched")
            }
            complete("")
            //            complete(HttpEntity(ContentTypes.`application/json`, s"<h1>Event of type [${event.event_type.attack_class}] receive successfully</h1>"))
          }
        }
      },
    )
  }
}
