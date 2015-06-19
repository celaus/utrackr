package de.afs.platform.e2e

import authentikat.jwt.JsonWebToken
import com.github.nscala_time.time.Imports._
import de.afs.platform.common.config.AuthenticationConfiguration
import de.afs.platform.common.utils.UUIDMixIn
import de.afs.platform.domain.User
import de.afs.platform.logic.managers.JWTokenManagerComponent
import de.afs.platform.logic.{TokenManager, UserManager}
import de.afs.platform.service.web.UserServiceApi
import de.afs.platform.service.web.dto.{TokenDataResponse, UserDataResponse}
import de.afs.platform.service.web.json.DomainObjectProtocol._
import org.specs2.mutable.Specification
import spray.http.HttpHeaders.Authorization
import spray.http.{ContentTypes, HttpEntity, HttpMethods, OAuth2BearerToken, StatusCodes}
import spray.httpx.SprayJsonSupport._
import spray.routing._
import spray.testkit.Specs2RouteTest


class UserServiceSpec extends Specification with Specs2RouteTest with UUIDMixIn {
  isolated

  var userDbLookups = 0

  val aUser = new User(1, "user@user.com", "mypassword", false, None)

  var persistentUser: User = _

  val testDate = DateTime.now.withYear(2099).month(12).day(4).hour(1).minute(0).second(0)
  // please update 2099
  val algorithm = "HS256"
  val tokenSecret = "secret"

  val tokenManagerComponent = new JWTokenManagerComponent with AuthenticationConfiguration {

    override def secret: String = tokenSecret

    override def algorithmName: String = algorithm

    override def tokenExpiration: Duration = Period.days(2).toStandardDuration

  }
  val tokenManager = tokenManagerComponent.tokenManager

  val userService = new UserServiceApi {

    override val tokenManager: TokenManager = tokenManagerComponent.tokenManager

    override val userManager: UserManager = new UserManager {

      override def save(user: User): User = { println(s"hello $user"); persistentUser = user; user }

      override def findUserById(id: Integer): Option[User] = if (id == aUser.id) Some(aUser) else None

      override def findUserByMailAndPassword(email: String, password: String): Option[User] =
        if ((aUser.email == email) && (aUser.password == password)) Some(aUser)
        else None

      override def findUserByToken(token: String): Option[User] = if (token == aUser.token) Some(aUser) else None

      override def delete(user: User): User = user.copy(id = 0)

      override def resetPasswordOf(user: User): User = {persistentUser = user.copy(passwordReset = true, password = "", token = Some(newId)); persistentUser }

      override def findUserByMail(email: String): Option[User] = if (aUser.email == email) Some(aUser) else None
    }

    override val authenticationConfig: AuthenticationConfiguration = new AuthenticationConfiguration {

      override def secret: String = tokenSecret

      override def algorithmName: String = algorithm

      override def tokenExpiration: Duration = Period.days(2).toStandardDuration

    }
  }

  "A User Service" should {
    "/user/login" should {
      "provide a valid token for a valid email/password combination" in {

        Post("/user/login").withEntity(HttpEntity(ContentTypes.`application/json`, s"""{"email": "${aUser.email}", "password": "${aUser.password}"}""")) ~> userService.userServiceRoute ~> check {
          status === StatusCodes.OK
          JsonWebToken.validate(responseAs[TokenDataResponse].token, tokenSecret) must beTrue
        }
      }

      "reject invalid email/password combinations" in {

        Post("/user/login").withEntity(HttpEntity(ContentTypes.`application/json`, s"""{"email": "not@a-user.com", "password": "invalid"}""")) ~> userService.userServiceRoute ~> check {
          rejections === List(AuthenticationFailedRejection(AuthenticationFailedRejection.CredentialsRejected, List()))
        }
      }

      "reject when no body is sent" in {

        Post("/user/login") ~> userService.userServiceRoute ~> check {
          rejections === List(RequestEntityExpectedRejection)
        }
      }

      "reject content types other than 'application/json'" in {

        Post("/user/login").withEntity(HttpEntity(ContentTypes.`text/plain(UTF-8)`, s"""{"email": "${aUser.email}", "password": "${aUser.password}"}""")) ~> userService.userServiceRoute ~> check {
          rejections === List(UnsupportedRequestContentTypeRejection("Expected 'application/json'"))
        }

        Post("/user/login").withEntity(HttpEntity(ContentTypes.NoContentType, s"""{"email": "${aUser.email}", "password": "${aUser.password}"}""")) ~> userService.userServiceRoute ~> check {
          rejections === List(UnsupportedRequestContentTypeRejection("Expected 'application/json'"))
        }

        Post("/user/login").withEntity(HttpEntity(ContentTypes.`application/octet-stream`, s"""{"email": "${aUser.email}", "password": "${aUser.password}"}""")) ~> userService.userServiceRoute ~> check {
          rejections === List(UnsupportedRequestContentTypeRejection("Expected 'application/json'"))
        }
      }

      "reject anything other than POST requests" in {

        Get("/user/login") ~> userService.userServiceRoute ~> check {
          rejections === List(MethodRejection(HttpMethods.POST))
        }

        Patch("/user/login") ~> userService.userServiceRoute ~> check {
          rejections === List(MethodRejection(HttpMethods.POST))
        }

        Put("/user/login") ~> userService.userServiceRoute ~> check {
          rejections === List(MethodRejection(HttpMethods.POST))
        }

        Options("/user/login") ~> userService.userServiceRoute ~> check {
          rejections === List(MethodRejection(HttpMethods.POST))
        }

        Delete("/user/login") ~> userService.userServiceRoute ~> check {
          rejections === List(MethodRejection(HttpMethods.POST))
        }

        Head("/user/login") ~> userService.userServiceRoute ~> check {
          rejections === List(MethodRejection(HttpMethods.POST))
        }

      }
    }

    "/user/<id>" should {
      val token = tokenManager.createToken(aUser)

      "return an existing user's email address and user id" in {

        Get("/user/1").withHeaders(Authorization(new OAuth2BearerToken(token))) ~> userService.userServiceRoute ~> check {
          val response = responseAs[UserDataResponse]
          response.email must_== (aUser.email)
          response.id.id must_== (aUser.id.toLong)
        }
      }

      "reject requests without valid authorization headers" in {
        Get(s"/user/1").withHeaders(Authorization(new OAuth2BearerToken("not-a-token"))) ~> userService.userServiceRoute ~> check {
          rejections === List(AuthenticationFailedRejection(AuthenticationFailedRejection.CredentialsRejected, List()))
        }

        Get(s"/user/1") ~> userService.userServiceRoute ~> check {
          rejections === List(AuthenticationFailedRejection(AuthenticationFailedRejection.CredentialsMissing, List()))
        }
      }

      "reject requests for other ids" in {

        Get(s"/user/0").withHeaders(Authorization(new OAuth2BearerToken(token))) ~> userService.userServiceRoute ~> check {
          rejections === List(AuthorizationFailedRejection)
        }

        Get(s"/user/2").withHeaders(Authorization(new OAuth2BearerToken(token))) ~> userService.userServiceRoute ~> check {
          rejections === List(AuthorizationFailedRejection)

        }

        Get(s"/user/13456").withHeaders(Authorization(new OAuth2BearerToken(token))) ~> userService.userServiceRoute ~> check {
          rejections === List(AuthorizationFailedRejection)
        }

        Get(s"/user/123").withHeaders(Authorization(new OAuth2BearerToken(token))) ~> userService.userServiceRoute ~> check {
          rejections === List(AuthorizationFailedRejection)
        }
      }
    }
    "/user/reset" should {

      "set a UUID token in the corresponding user object" in {

        Post("/user/reset").withEntity(HttpEntity(ContentTypes.`application/json`, s"""{"email": "${aUser.email}"}""")) ~> userService.userServiceRoute ~> check {
          status === StatusCodes.OK

          persistentUser.token must not beEmpty

          persistentUser.password must be empty

          persistentUser.passwordReset must beTrue

          isAValidId(persistentUser.token.get) must beTrue
        }
      }

      "reject invalid email addresses" in {

        Post("/user/reset").withEntity(HttpEntity(ContentTypes.`application/json`, s"""{"email": "not@a-user.com"}""")) ~> userService.userServiceRoute ~> check {
          rejections === List(AuthenticationFailedRejection(AuthenticationFailedRejection.CredentialsRejected, List()))
        }
      }

      "reject when no body is sent" in {

        Post("/user/reset") ~> userService.userServiceRoute ~> check {
          rejections === List(RequestEntityExpectedRejection)
        }
      }

      "reject content types other than 'application/json'" in {

        Post("/user/reset").withEntity(HttpEntity(ContentTypes.`text/plain(UTF-8)`, s"""{"email": "${aUser.email}"}""")) ~> userService.userServiceRoute ~> check {
          rejections === List(UnsupportedRequestContentTypeRejection("Expected 'application/json'"))
        }

        Post("/user/reset").withEntity(HttpEntity(ContentTypes.NoContentType, s"""{"email": "${aUser.email}"}""")) ~> userService.userServiceRoute ~> check {
          rejections === List(UnsupportedRequestContentTypeRejection("Expected 'application/json'"))
        }

        Post("/user/reset").withEntity(HttpEntity(ContentTypes.`application/octet-stream`, s"""{"email": "${aUser.email}"}""")) ~> userService.userServiceRoute ~> check {
          rejections === List(UnsupportedRequestContentTypeRejection("Expected 'application/json'"))
        }
      }
    }
  }
}