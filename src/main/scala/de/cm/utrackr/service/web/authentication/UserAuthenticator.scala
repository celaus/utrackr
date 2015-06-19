package de.cm.utrackr.service.web.authentication

import com.github.nscala_time.time.Imports._
import de.afs.platform.domain.AuthenticationInfo
import de.afs.platform.logic.UserManager
import de.afs.platform.logic.managers.JWTClaimsAttributes

import scala.concurrent.{ExecutionContext, Future}


trait UserAuthenticator extends JWTClaimsAttributes {


  def userManager: UserManager

  def authenticator(claims: Option[Map[String, String]])(implicit ec: ExecutionContext): Future[Option[AuthenticationInfo]] = Future {
    val claimsMap = claims.getOrElse(Map[String, String]())

    val expirationValue = claimsMap.getOrElse(ExpirationHeader, DateTime.now.getMillis.toString).toLong
    val expiration = new DateTime(expirationValue)

    if (expiration.isAfterNow) {
      val user = userManager.findUserById(claimsMap.getOrElse(UserIdHeader, "0").toInt)

      if (user.isDefined && user.get.email == claimsMap.getOrElse(UserMailHeader, ""))
        Option(new AuthenticationInfo(user.get, expiration))
      else None
    }
    else None
  }
}
