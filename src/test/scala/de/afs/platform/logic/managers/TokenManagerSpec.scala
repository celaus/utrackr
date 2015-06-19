package de.afs.platform.logic.managers

  import authentikat.jwt.JsonWebToken
  import com.github.nscala_time.time.Imports._
  import de.afs.platform.common.config.AuthenticationConfiguration
  import de.afs.platform.domain.User
  import org.specs2.mutable._
  import org.specs2.time.NoTimeConversions

  class TokenManagerSpec extends Specification with JWTClaimsAttributes with NoTimeConversions {
    isolated

    val timeStampMaxLength = 9
  var userDbLookups = 0

  val aUser = new User(1, "user@user.com", "mypassword", false, None)

  val testDate = DateTime.now.withYear(2099).month(12).day(4).hour(1).minute(0).second(0)
  // please update 2099
  val algorithm = "HS256"
  val tokenSecret = "secret"

  val tokenManager = new JWTokenManagerComponent with AuthenticationConfiguration {

    override def secret: String = tokenSecret

    override def algorithmName: String = algorithm

    override def tokenExpiration: Duration = Period.days(2).toStandardDuration

  }.tokenManager


  "An AuthenticationManager" should {
    "have a method createToken that" should {

      "return a token that is recognized as valid" in {
        val authToken = tokenManager.createToken(aUser)
        JsonWebToken.validate(authToken, tokenSecret) must beTrue
      }

      "return a token with a claims set that includes user id, email, and an expiration date in the future" in {
        val authToken = tokenManager.createToken(aUser)
        JsonWebToken.validate(authToken, tokenSecret) must beTrue
        authToken match {
          case JsonWebToken(header, claims, signature) =>
            val claimsMap = claims.asSimpleMap.get
            DateTime.parse(claimsMap.get(ExpirationHeader).get take timeStampMaxLength).isAfterNow must beTrue
            claimsMap.get(UserIdHeader) must beSome(aUser.id.toString)
            claimsMap.get(UserMailHeader) must beSome(aUser.email)
        }
      }
    }
    /*
      "check if a user exists before a token is created" in {
        val dbLookups = userDbLookups
        val authToken = authenticationManager.createToken(aUser).get
        userDbLookups must_== (dbLookups + 1)
      }

      authenticationManager.validate(authToken.signedToken) must_== (Some(authToken))
      "check if a user exists when validating a token" in {
        val authToken = authenticationManager.createToken(aUser).get
        val dbLookups = userDbLookups

        userDbLookups must_== (dbLookups + 1)
      }


      "must throw an Exception on invalid tokens" in {
        authenticationManager.validate("not-a-token") must throwA[InvalidAuthenticationTokenException]
      }

      "return None if the signature does not match the payload" in {
        val invalidJWTToken = new AuthenticationInfo(aUser, testDate, algorithm, Map(
          "exp" -> testDate.toString(ISODateTimeFormat.dateTime()),
          "userId" -> "2",
          "userMail" -> aUser.email),
          "asd",
          "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyTWFpbCI6InVzZXJAdXNlci5jb20iLCJ1c2VySWQiOiIxIiwiZXhwIjoiMjA5OS0xMi0wNFQwMTowMDowMC4wMDArMDE6MDAifQ.asd")

        authenticationManager.validate(invalidJWTToken.signedToken) must beNone
      }

      "return None if the provided id does not match the email address" in {

        val invalidJWTToken = new AuthenticationInfo(aUser, testDate, algorithm, Map(
          "exp" -> testDate.toString(ISODateTimeFormat.dateTime()),
          "userId" -> aUser.id.toString,
          "userMail" -> "some@other.mail"),
          "eEeR4LEiwDJnBQ9mqiNR_597xn-QTg5aIFoyPZqHLaI",
          "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyTWFpbCI6InNvbWVAb3RoZXIubWFpbCIsInVzZXJJZCI6IjEiLCJleHAiOiIyMDk5LTEyLTA0VDAxOjAwOjAwLjAwMCswMTowMCJ9.eEeR4LEiwDJnBQ9mqiNR_597xn-QTg5aIFoyPZqHLaI")

        authenticationManager.validate(invalidJWTToken.signedToken) must beNone
      }



      "return None on expired tokens" in {
        val old = new DateTime().withYear(2000).month(12).day(4).hour(1).minute(0).second(0)
        val invalidJWTToken = new AuthenticationInfo(aUser, old, algorithm, Map(
          "exp" -> old.toString(ISODateTimeFormat.dateTime()),
          "userId" -> aUser.id.toString,
          "userMail" -> aUser.email),
          "nKwctRFglmWHK06VLMBotrdxFmBwigLue5gEmbEtoJM",
          "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyTWFpbCI6InVzZXJAdXNlci5jb20iLCJ1c2VySWQiOiIxIiwiZXhwIjoiMjAwMC0xMi0wNFQwMTowMDowMC4wMDArMDE6MDAifQ.nKwctRFglmWHK06VLMBotrdxFmBwigLue5gEmbEtoJM")

        authenticationManager.validate(invalidJWTToken.signedToken) must beNone
      }
      */
  }
}
