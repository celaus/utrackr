package de.cm.utrackr.common.jwt


trait JWTClaimsAttributes {
  val ExpirationHeader = "exp"
  val UserIdHeader = "userId"
  val UserMailHeader = "userMail"
}
