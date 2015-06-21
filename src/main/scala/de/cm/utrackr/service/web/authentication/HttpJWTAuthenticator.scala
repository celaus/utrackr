package de.cm.utrackr.service.web.authentication

import authentikat.jwt._
import com.fasterxml.jackson.core.JsonParseException
import com.typesafe.scalalogging.LazyLogging
import de.cm.utrackr.common.config.AuthenticationConfiguration
import spray.http.{HttpCredentials, HttpRequest, OAuth2BearerToken}
import spray.routing.RequestContext
import spray.routing.authentication.HttpAuthenticator

import scala.concurrent.{ExecutionContext, Future}

/**
 * Implements HTTP-Header-based authentication with a JSON Web Token to use in Spray.
 * @param tokenAuthenticator The authenticator method.
 * @param realm A resource or domain where access is requested.
 * @param authenticationConfig The configuration for JWT (secret, algorithm).
 * @param executionContext ... Spray stuff
 * @tparam U ... Spray stuff
 */
class HttpJWTAuthenticator[U](val tokenAuthenticator: TokenAuthenticator[U], val realm: String, val authenticationConfig: AuthenticationConfiguration)(implicit val executionContext: ExecutionContext)
  extends HttpAuthenticator[U] with LazyLogging {

  def getChallengeHeaders(httpRequest: HttpRequest) = List()

  override def authenticate(credentials: Option[HttpCredentials], ctx: RequestContext): Future[Option[U]] = try {
    tokenAuthenticator {
      logger.info("Incoming credentials: " + credentials.getOrElse("").toString)
      credentials.getOrElse(None) match {
        case t: OAuth2BearerToken => if (JsonWebToken.validate(t.token, authenticationConfig.secret))
          t.token match {
            case JsonWebToken(header, claims, signature) => val a = Some(claims.asSimpleMap.get); logger.info(a.get.toString()); a
            case _ => None
          }
        else None
        case _ => None
      }
    }
  }
  catch {
    case e: JsonParseException => Future(None)
  }
}

/**
 *
 */
object JWTAuthentication {
  def apply[U](tokenAuthenticator: TokenAuthenticator[U], realm: String, authenticationConfig: AuthenticationConfiguration)(implicit ec: ExecutionContext): HttpAuthenticator[U] =
    new HttpJWTAuthenticator[U](tokenAuthenticator, realm, authenticationConfig)
}
