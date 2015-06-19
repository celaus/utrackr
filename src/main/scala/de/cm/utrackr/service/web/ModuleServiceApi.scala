package de.cm.utrackr.service.web

import com.typesafe.scalalogging.LazyLogging
import de.afs.platform.common.config.AuthenticationConfiguration
import de.afs.platform.common.utils.EncodedId
import de.afs.platform.domain.{AuthenticationInfo, ModuleSession, ItemWithCompletedAsset, Module}
import de.afs.platform.logic.{EmotionManager, TrialManager, ModuleSessionManager, ModuleSessionOptions}
import de.afs.platform.service.web.authentication.{JWTAuthentication, UserAuthenticator}
import de.afs.platform.service.web.dto._
import de.afs.platform.service.web.json.DomainObjectProtocol._
import de.afs.platform.service.web.utils.PathMatchers.HashEncodedId
import spray.httpx.SprayJsonSupport._
import spray.routing.AuthenticationFailedRejection.CredentialsRejected
import spray.routing.HttpService._
import spray.routing._

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Random

/**
 * A trait for implementing a Service API.
 */
trait ModuleServiceApiImplementation {

  val authenticationRealm: String

  /**
   * Creates a new session.
   * @param authInfo Requestee.
   * @param request The request content.
   * @return A Spray route to execute.
   */
  def newSession(authInfo: AuthenticationInfo, request: NewGameSessionRequest): Route

  /**
   * Tries to retrieve the next Trials to finish.
   * @param authInfo Requestee.
   * @param session The associated session.
   * @return A Spray route to execute.
   */
  def nextTrial(authInfo: AuthenticationInfo, session: ModuleSession): Route

  /**
   * Checks if the passed trials are correct.
   * @param authInfo Requestee.
   * @param session The associated session.
   * @param checkGameResultRequest The request content.
   * @return A Spray route to execute.
   */
  def checkTrials(authInfo: AuthenticationInfo, session: ModuleSession, checkGameResultRequest: CheckGameResultRequest): Route
}

trait ModuleServiceApi extends UserAuthenticator with LazyLogging {

  val moduleSessionManager: ModuleSessionManager
  val authenticationConfig: AuthenticationConfiguration
  val emotionManager: EmotionManager

  case class DefaultModuleServiceApiImplementation(currentModule: Module, trialManager: TrialManager, authenticationRealm: String) extends ModuleServiceApiImplementation {


    def newSession(authInfo: AuthenticationInfo, request: NewGameSessionRequest): Route = {
      println(request)
      val x = moduleSessionManager.createSession(currentModule, authInfo.user, ModuleSessionOptions(request.session, request.trials, request.difficulty))
      println(x)
      x match {
        case Some(session: ModuleSession) => complete(NewGameSessionResponse(EncodedId(session.id)))
        case _ => reject(MalformedRequestContentRejection(request.toString))
      }
    }


    def nextTrial(authInfo: AuthenticationInfo, session: ModuleSession): Route = {
      val distractorCount = 1;
      val assetMap = mutable.Map[String, Seq[AssetResponse]]().withDefault(_ => List())
      val nextTrials = trialManager.nextTrials(session)
      val trialEmotions = nextTrials map (t => emotionManager.emotionForTrial(t._1).get)

      val emotions = nextTrials map (t => EncodedId(t._1.id)) zip (trialEmotions map (_.name))

      // Get the highest trial id and increment to avoid duplicate trial ids
      val ids = (1 to distractorCount) map {
        i => EncodedId(i + (Int.MinValue /: nextTrials) { (p, c) => c._1.id max p })
      }

      val addedEmotions = ids zip (emotionManager.getEmotions(distractorCount, (e) => !trialEmotions.contains(e)) map (_.name))

      (assetMap /: nextTrials) { (result, t) => {
        val assets = t._2
        val trial = t._1

        assets foreach { a =>
          val key = a.puzzleKey.getOrElse("")
          result.update(key, result(key) ++ Seq(AssetResponseUtil.fromAsset(a, Some(trial))))
        }
        result
      }
      }
      complete(NextTrialsResponse(EncodedId(session.id.toLong), assetMap.toMap, new Random().shuffle(emotions ++ addedEmotions)))

    }


    def checkTrials(authInfo: AuthenticationInfo, session: ModuleSession, checkGameResultRequest: CheckGameResultRequest): Route = {
      val trial = trialManager.trialDataForId(session, checkGameResultRequest.trialId.id.toInt)

      if (!trial.isDefined) reject(MalformedRequestContentRejection(checkGameResultRequest.trialId.encoded))
      else {
        val solution = trialManager.trySolve(trial.get, checkGameResultRequest.assetIds map (_.id.toInt))
        val assetResponse = solution._2 match {
          case Some(itemWithAsset) => Some (AssetResponseUtil.fromAsset (itemWithAsset.completedAsset) )
          case _ => None
        }

        complete(CheckGameResultResponse(assetResponse, checkGameResultRequest.assetIds, solution._1))
      }
    }
  }

  /**
   * Assemble the routes using the provided implementation and parameters.
   * @param prefix The path where the modules should be delivered.
   * @param modulePath The name for the module path.
   * @return A Spray route to execute.
   */
  def makeRoutes(prefix: String, modulePath: String, moduleServiceApiImpl: ModuleServiceApiImplementation) = {
    pathPrefix(prefix / modulePath) {
      pathEndOrSingleSlash {
        post {
          authenticate(JWTAuthentication(authenticator, moduleServiceApiImpl.authenticationRealm, authenticationConfig)) { authInfo =>
            entity(as[NewGameSessionRequest]) { r => moduleServiceApiImpl.newSession(authInfo, r) }
          }
        }
      } ~
        pathPrefix(HashEncodedId / "trial" / "next") { sessionId =>
          pathEndOrSingleSlash {
            get {
              authenticate(JWTAuthentication(authenticator, moduleServiceApiImpl.authenticationRealm, authenticationConfig)) { authInfo =>
                moduleSessionManager.byId(sessionId.id.toInt) match {
                  case Some(session: ModuleSession) if session.userId == authInfo.user.id => moduleServiceApiImpl.nextTrial(authInfo, session)
                  case _ => reject(AuthenticationFailedRejection.apply(CredentialsRejected, List()))
                }
              }
            }
          }
        } ~
        pathPrefix(HashEncodedId / "check") { sessionId =>
          pathEndOrSingleSlash {
            post {
              authenticate(JWTAuthentication(authenticator, moduleServiceApiImpl.authenticationRealm, authenticationConfig)) { authInfo =>
                entity(as[CheckGameResultRequest]) { checkGameResultRequest =>
                  moduleSessionManager.byId(sessionId.id.toInt) match {
                    case Some(session: ModuleSession) if session.userId == authInfo.user.id => moduleServiceApiImpl.checkTrials(authInfo, session, checkGameResultRequest)
                    case _ => reject(AuthenticationFailedRejection.apply(CredentialsRejected, List()))
                  }
                }
              }
            }
          }
        }
    }
  }
}
