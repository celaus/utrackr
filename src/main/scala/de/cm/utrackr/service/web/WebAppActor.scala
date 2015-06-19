package de.cm.utrackr.service.web

import akka.actor.{Actor, ActorContext}
import com.typesafe.scalalogging.LazyLogging
import de.afs.platform.common.config.AuthenticationConfiguration
import de.afs.platform.domain.Module
import de.afs.platform.logic._
import de.afs.platform.service.web.modules.{FilmPuzzleModuleServiceApi, WhoSpeaksModuleServiceApi, FacePuzzleModuleServiceApi}
import de.afs.platform.service.web.utils.CORSSupport
import spray.http.{StatusCodes, StatusCode}
import spray.routing.HttpService._
import spray.routing.RejectionHandler.Default
import spray.routing._


case class WebAppActor(userManager: UserManager,
                       authenticationConfig: AuthenticationConfiguration,
                       tokenManager: TokenManager,
                       moduleSessionManager: ModuleSessionManager,
                       moduleManager: ModuleManager,
                       emotionManager: EmotionManager,
                       modulesPrefix: String,
                       facePuzzleModule: Module,
                       facePuzzleTrialManager: TrialManager,
                       whoSpeaksModule: Module,
                       whoSpeaksTrialManager: TrialManager,
                       filmPuzzleModule: Module,
                       filmPuzzleTrialManager: TrialManager) extends Actor with HttpService with WebServiceApiComposition with CORSSupport {


  implicitly[RoutingSettings]
  implicitly[ExceptionHandler]

  def actorRefFactory: ActorContext = context

  override def receive: Receive = runRoute(cors {
    route
  })
}


trait WebServiceApiComposition extends UserServiceApi with FacePuzzleModuleServiceApi with WhoSpeaksModuleServiceApi with FilmPuzzleModuleServiceApi with DebugServiceApi {

  lazy val route = debugServiceRoute ~ userServiceRoute ~ facePuzzleServiceRoute ~ whoSpeaksServiceRoute ~ filmPuzzleServiceRoute
}