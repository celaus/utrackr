package de.cm.utrackr.service.web

import akka.actor.{Actor, ActorContext}
import de.cm.utrackr.common.config.AuthenticationConfiguration
import de.cm.utrackr.logic.{TokenManager, UserManager}
import de.cm.utrackr.service.web.utils.CORSSupport
import spray.routing.RejectionHandler.Default
import spray.routing._


case class WebAppActor(userManager: UserManager,
                       tokenManager: TokenManager,
                       authenticationConfig: AuthenticationConfiguration) extends Actor with HttpService with WebServiceApiComposition with CORSSupport {


  implicitly[RoutingSettings]
  implicitly[ExceptionHandler]

  def actorRefFactory: ActorContext = context

  override def receive: Receive = runRoute(cors {
    route
  })
}


trait WebServiceApiComposition extends UserServiceApi {

  lazy val route = userServiceRoute
}