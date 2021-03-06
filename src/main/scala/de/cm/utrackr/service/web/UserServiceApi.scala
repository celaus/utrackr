package de.cm.utrackr.service.web

import de.cm.utrackr.common.config.AuthenticationConfiguration
import de.cm.utrackr.common.utils.EncodedId
import de.cm.utrackr.logic.{TokenManager, UserManager}
import de.cm.utrackr.service.web.authentication.{JWTAuthentication, UserAuthenticator}
import de.cm.utrackr.service.web.dto.{LoginDataRequest, ResetPasswordDataRequest, TokenDataResponse, UserDataResponse}
import spray.routing.HttpService._
import spray.routing._
import de.cm.utrackr.service.web.json.DomainObjectProtocol._
import spray.httpx.SprayJsonSupport._


import scala.concurrent.ExecutionContext.Implicits.global

trait UserServiceApi extends UserAuthenticator {

  val userManager: UserManager
  val tokenManager: TokenManager
  val authenticationConfig: AuthenticationConfiguration


  lazy val userServiceRoute =
    pathPrefix("user" / "login") {
      pathEndOrSingleSlash {
        post {
          entity(as[LoginDataRequest]) { loginData =>
            val userFound = userManager.findUserByMailAndPassword(loginData.email, loginData.password)

            userFound match {
              case Some(user) => complete(new TokenDataResponse(tokenManager.createToken(user)))
              case _ => reject(AuthenticationFailedRejection(AuthenticationFailedRejection.CredentialsRejected, List()))
            }
          }
        }
      }
    } ~
      pathPrefix("user" / IntNumber) { userId =>
        authenticate(JWTAuthentication(authenticator, "users", authenticationConfig)) { authInfo =>
          pathEndOrSingleSlash {
            get {
              if (userId == authInfo.user.id)
                complete {
                  val user = userManager.findUserById(userId).get
                  UserDataResponse(EncodedId(user.id), user.email)
                }
              else
                reject(AuthorizationFailedRejection)
            }
          }
        }
      } ~
      path("user" / "reset") {
        post {
          entity(as[ResetPasswordDataRequest]) { resetPasswordData =>
            val userFound = userManager.findUserByMail(resetPasswordData.email)

            userFound match {
              case Some(user) =>
                userManager.resetPasswordOf(user);
                complete {
                  "" // empty response
                }
              case _ => reject(AuthenticationFailedRejection(AuthenticationFailedRejection.CredentialsRejected, List()))
            }
          }
        }
      }
}
