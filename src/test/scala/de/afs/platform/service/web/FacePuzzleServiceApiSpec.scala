package de.afs.platform.service.web

import com.github.nscala_time.time.Imports.{DateTime, Duration, Period}
import com.sun.xml.internal.ws.encoding.soap.DeserializationException
import de.afs.platform.common.config.AuthenticationConfiguration
import de.afs.platform.common.utils.{UUIDMixIn, EncodedId}
import de.afs.platform.domain._
import de.afs.platform.logic._
import de.afs.platform.service.web.dto._
import de.afs.platform.service.web.json.DomainObjectProtocol._
import de.afs.platform.service.web.modules.FacePuzzleModuleServiceApi
import de.afs.platform.test.utils.ManagerCreator
import org.specs2.matcher.MatcherMacros
import org.specs2.mutable.Specification
import spray.http.HttpHeaders.Authorization
import spray.http._
import spray.httpx.SprayJsonSupport._
import spray.routing.AuthenticationFailedRejection.CredentialsRejected
import spray.routing._
import spray.json.DeserializationException
import spray.testkit.Specs2RouteTest
import scala.collection.mutable
import scala.language.experimental.macros

class FacePuzzleServiceApiSpec extends Specification with Specs2RouteTest with MatcherMacros with UUIDMixIn {

  val difficulty = 1
  val trials = 2
  val length = 2

  val prefix = "module"
  val modulePath = "facepuzzle"

  val aUser = User(1, "user@user.com", "mypassword", false, None)
  val anotherUser = User(2, "b@c.com", "a", false, None)

  val emotion = Emotion(1, "An Emotion")


  val allEmotions = List(
    emotion,
    new Emotion(2, "An Emotion2"),
    new Emotion(3, "An Emotion3"),
    new Emotion(4, "An Emotion4"))

  val completed = Asset(1, "video/webm", "/a/b.mov", 0, None, None)
  val module = Module(1, "Module")

  val item = Item(1, 2, module.id, Some(completed.id), emotion.id)

  val itemA = Item(2, 2, module.id, Some(completed.id), allEmotions(1).id)
  val itemB = Item(3, 2, module.id, Some(completed.id), allEmotions(2).id)
  val itemC = Item(4, 2, module.id, Some(completed.id), allEmotions(3).id)


  val currentSession = ModuleSession(1, module.id, aUser.id, difficulty, trials, length)


  val keyA = "A"
  val keyB = "B"

  val trialObjects = List(
    (TrialData(1, 1, itemA.id, currentSession.id),
      List(Asset(2, "video/webm", "/a/b.mov", 0, Some(itemA.id), Some(keyA)),
        Asset(3, "video/webm", "/a/b.mov", 0, Some(itemA.id), Some(keyB)))),
    (TrialData(2, 2, itemB.id, currentSession.id),
      List(Asset(4, "video/webm", "/a/b.mov", 0, Some(itemB.id), Some(keyA)),
        Asset(5, "video/webm", "/a/b.mov", 0, Some(itemB.id), Some(keyB)))),
    (TrialData(3, 3, itemC.id, currentSession.id),
      List(Asset(6, "video/webm", "/a/b.mov", 0, Some(itemC.id), Some(keyA)),
        Asset(7, "video/webm", "/a/b.mov", 0, Some(itemC.id), Some(keyB))))
  )

  val matchingTrials = List(1, 2)

  val testDate = DateTime.now
    .withYear(2099) // please update 2099
    .withMonthOfYear(12)
    .withDayOfMonth(4)
    .withHourOfDay(1)
    .withMinuteOfHour(0)
    .withSecondOfMinute(0)

  val algorithm = "HS256"
  val tokenSecret = "secret"

  val tokenManagerComponent = ManagerCreator.createTokenManager(tokenSecret, algorithm, Period.days(2).toStandardDuration)
  val tokenManager = tokenManagerComponent.tokenManager
  val testUserManager = ManagerCreator.createUserManager(aUser, newId, List(aUser, anotherUser))

  val trialManager = ManagerCreator.createTrialManager(currentSession, trialObjects, matchingTrials, ItemWithCompletedAsset(item, completed))

  val facePuzzleApi = new ModuleServiceApi {

    override val emotionManager: EmotionManager = ManagerCreator.createEmotionManager(allEmotions)

    override val authenticationConfig = new AuthenticationConfiguration {

      override def secret: String = tokenSecret

      override def algorithmName: String = algorithm

      override def tokenExpiration: Duration = Period.days(2).toStandardDuration
    }

    override val moduleSessionManager: ModuleSessionManager = ManagerCreator.createModuleSessionManager(
      currentSession,
      emotion)


    override def userManager = testUserManager
  }

  val impl = facePuzzleApi.DefaultModuleServiceApiImplementation(module, trialManager, "realm")

  "A FacePuzzle Service" should {
    val moduleFacepuzzlePath = f"/$prefix/$modulePath"
    val nextFacepuzzlePath = f"/$prefix/$modulePath/" + "%s/trial/next"
    val checkFacepuzzlePath = f"/$prefix/$modulePath/" + "%s/check"

    val token = tokenManager.createToken(aUser)
    val otherToken = tokenManager.createToken(anotherUser)

    f"/$prefix/$modulePath" should {

      "return a new gameId for valid game options" in {

        Post(moduleFacepuzzlePath)
          .withHeaders(Authorization(OAuth2BearerToken(token)))
          .withEntity(HttpEntity(ContentTypes.`application/json`, s"""{
              "session": $length,
              "trials": $trials,
              "difficulty": $difficulty
            }""")) ~> facePuzzleApi.makeRoutes(prefix, modulePath, impl) ~> check {
          status === StatusCodes.OK
          responseAs[NewGameSessionResponse] must_== NewGameSessionResponse(EncodedId(currentSession.id))
        }
      }

      "reject invalid/missing game option types" in {

        Post(moduleFacepuzzlePath)
          .withHeaders(Authorization(OAuth2BearerToken(token)))
          .withEntity(HttpEntity(ContentTypes.`application/json`, s"""{
              "session": "asd",
              "trials": 2,
              "difficulty": 5
            }""")) ~> facePuzzleApi.makeRoutes(prefix, modulePath, impl) ~> check {

          rejections must haveLength(1)
          rejections(0).isInstanceOf[MalformedRequestContentRejection] must beTrue
        }

        Post(moduleFacepuzzlePath)
          .withHeaders(Authorization(OAuth2BearerToken(token)))
          .withEntity(HttpEntity(ContentTypes.`application/json`, s"""{
              "session": 5,
              "trials": 2,
              "difficulty": "asd"
            }""")) ~> facePuzzleApi.makeRoutes(prefix, modulePath, impl) ~> check {
          rejections must haveLength(1)
          rejections(0).isInstanceOf[MalformedRequestContentRejection] must beTrue
        }

        Post(moduleFacepuzzlePath)
          .withHeaders(Authorization(OAuth2BearerToken(token)))
          .withEntity(HttpEntity(ContentTypes.`application/json`, s"""{
              "session": 5,
              "trials": "asd",
              "difficulty": 5
            }""")) ~> facePuzzleApi.makeRoutes(prefix, modulePath, impl) ~> check {
          rejections must haveLength(1)
          rejections(0).isInstanceOf[MalformedRequestContentRejection] must beTrue
        }

        Post(moduleFacepuzzlePath)
          .withHeaders(Authorization(OAuth2BearerToken(token)))
          .withEntity(HttpEntity(ContentTypes.`application/json`, s"""{
              "session": 5,
              "difficulty": 5
            }""")) ~> facePuzzleApi.makeRoutes(prefix, modulePath, impl) ~> check {
          rejections must haveLength(1)
          rejections(0).isInstanceOf[MalformedRequestContentRejection] must beTrue
        }

        Post(moduleFacepuzzlePath)
          .withHeaders(Authorization(OAuth2BearerToken(token)))
          .withEntity(HttpEntity(ContentTypes.`application/json`, s"""{
              "trials": 5,
              "difficulty": 5
            }""")) ~> facePuzzleApi.makeRoutes(prefix, modulePath, impl) ~> check {
          rejections must haveLength(1)
          rejections(0).isInstanceOf[MalformedRequestContentRejection] must beTrue
        }

        Post(moduleFacepuzzlePath)
          .withHeaders(Authorization(OAuth2BearerToken(token)))
          .withEntity(HttpEntity(ContentTypes.`application/json`, s"""{
              "session": 5,
              "trials": 5
            }""")) ~> facePuzzleApi.makeRoutes(prefix, modulePath, impl) ~> check {
          rejections must haveLength(1)
          rejections(0).isInstanceOf[MalformedRequestContentRejection] must beTrue
        }

        Post(moduleFacepuzzlePath)
          .withHeaders(Authorization(OAuth2BearerToken(token)))
          .withEntity(HttpEntity(ContentTypes.`application/json`, s"""{}""")) ~> facePuzzleApi.makeRoutes(prefix, modulePath, impl) ~> check {
          rejections must haveLength(1)
          rejections(0).isInstanceOf[MalformedRequestContentRejection] must beTrue
        }
      }



      "reject unauthorized requests" in {

        Post(moduleFacepuzzlePath).withEntity(HttpEntity(ContentTypes.`application/json`, s"""{
              "session": 5,
              "trials": 2,
              "difficulty": 5
            }""")) ~> facePuzzleApi.makeRoutes(prefix, modulePath, impl) ~> check {
          rejections === List(AuthenticationFailedRejection(AuthenticationFailedRejection.CredentialsMissing, List()))
        }
      }

      "reject when no body is sent" in {

        Post(moduleFacepuzzlePath)
          .withHeaders(Authorization(OAuth2BearerToken(token))) ~> facePuzzleApi.makeRoutes(prefix, modulePath, impl) ~> check {
          rejections === List(RequestEntityExpectedRejection)
        }
      }

      "reject content types other than 'application/json'" in {

        Post(moduleFacepuzzlePath)
          .withHeaders(Authorization(OAuth2BearerToken(token)))
          .withEntity(HttpEntity(ContentTypes.`text/plain(UTF-8)`, s"""{
              "session": 5,
              "trials": 2,
              "difficulty": 5
            }""")) ~> facePuzzleApi.makeRoutes(prefix, modulePath, impl) ~> check {
          rejections === List(UnsupportedRequestContentTypeRejection("Expected 'application/json'"))
        }

        Post(moduleFacepuzzlePath)
          .withHeaders(Authorization(OAuth2BearerToken(token)))
          .withEntity(HttpEntity(ContentTypes.NoContentType, s"""{
              "session": 5,
              "trials": 2,
              "difficulty": 5
            }""")) ~> facePuzzleApi.makeRoutes(prefix, modulePath, impl) ~> check {
          rejections === List(UnsupportedRequestContentTypeRejection("Expected 'application/json'"))
        }

        Post(moduleFacepuzzlePath)
          .withHeaders(Authorization(OAuth2BearerToken(token)))
          .withEntity(HttpEntity(ContentTypes.`application/octet-stream`, s"""{
              "session": 5,
              "trials": 2,
              "difficulty": 5
            }""")) ~> facePuzzleApi.makeRoutes(prefix, modulePath, impl) ~> check {
          rejections === List(UnsupportedRequestContentTypeRejection("Expected 'application/json'"))
        }
      }

      "reject anything other than POST requests" in {

        Get(moduleFacepuzzlePath)
          .withHeaders(Authorization(OAuth2BearerToken(token))) ~> facePuzzleApi.makeRoutes(prefix, modulePath, impl) ~> check {
          rejections === List(MethodRejection(HttpMethods.POST))
        }

        Patch(moduleFacepuzzlePath)
          .withHeaders(Authorization(OAuth2BearerToken(token))) ~> facePuzzleApi.makeRoutes(prefix, modulePath, impl) ~> check {
          rejections === List(MethodRejection(HttpMethods.POST))
        }

        Put(moduleFacepuzzlePath)
          .withHeaders(Authorization(OAuth2BearerToken(token))) ~> facePuzzleApi.makeRoutes(prefix, modulePath, impl) ~> check {
          rejections === List(MethodRejection(HttpMethods.POST))
        }

        Options(moduleFacepuzzlePath)
          .withHeaders(Authorization(OAuth2BearerToken(token))) ~> facePuzzleApi.makeRoutes(prefix, modulePath, impl) ~> check {
          rejections === List(MethodRejection(HttpMethods.POST))
        }

        Delete(moduleFacepuzzlePath)
          .withHeaders(Authorization(OAuth2BearerToken(token))) ~> facePuzzleApi.makeRoutes(prefix, modulePath, impl) ~> check {
          rejections === List(MethodRejection(HttpMethods.POST))
        }

        Head(moduleFacepuzzlePath)
          .withHeaders(Authorization(OAuth2BearerToken(token))) ~> facePuzzleApi.makeRoutes(prefix, modulePath, impl) ~> check {
          rejections === List(MethodRejection(HttpMethods.POST))
        }

      }
    }

    f"/$prefix/$modulePath/<id>/trial/next" should {


      "reject invalid/unauthorized session ids" in {
        Get(String.format(nextFacepuzzlePath, "asdfasdf"))
          .withHeaders(Authorization(new OAuth2BearerToken(token))) ~> facePuzzleApi.makeRoutes(prefix, modulePath, impl) ~> check {
          rejections === List()
        }

        Get(String.format(nextFacepuzzlePath, "lejRej"))
          .withHeaders(Authorization(new OAuth2BearerToken(token))) ~> facePuzzleApi.makeRoutes(prefix, modulePath, impl) ~> check {
          handled must beTrue
        }

        Get(String.format(nextFacepuzzlePath, "lejRej"))
          .withHeaders(Authorization(new OAuth2BearerToken(otherToken))) ~> facePuzzleApi.makeRoutes(prefix, modulePath, impl) ~> check {
          rejections === List(AuthenticationFailedRejection.apply(CredentialsRejected, List()))
        }
      }

      "return matching trials for within a session" in {

        val assetMap = mutable.Map[String, Seq[AssetResponse]]().withDefault(_ => List())

        (assetMap /: (trialObjects take trials)) { (result, t) => {
          val assets = t._2
          val trial = t._1

          assets foreach { a =>
            val key = a.puzzleKey.getOrElse("")
            result.update(key, result(key) ++ Seq(AssetResponseUtil.fromAsset(a, Some(trial))))
          }
          result
        }
        }

        Get(String.format(nextFacepuzzlePath, EncodedId(currentSession.id).encoded))
          .withHeaders(Authorization(new OAuth2BearerToken(token))) ~> facePuzzleApi.makeRoutes(prefix, modulePath, impl) ~> check {
          val emotions = (trialObjects map (t => (EncodedId(t._1.id), allEmotions(t._1.id).name)) take trials).toSeq ++ List((EncodedId(trials + 1), emotion.name))

          val expected = NextTrialsResponse(EncodedId(currentSession.id), assetMap.toMap, emotions)
          val byId = { a: AssetResponse => a.id.id }
          val byFirst = { a: (EncodedId, String) => a._1.id }

          responseAs[NextTrialsResponse].sessionId must_== expected.sessionId
          responseAs[NextTrialsResponse].assets(keyA).sortBy(byId) must_== expected.assets(keyA).sortBy(byId)
          responseAs[NextTrialsResponse].assets(keyB).sortBy(byId) must_== expected.assets(keyB).sortBy(byId)
          responseAs[NextTrialsResponse].assets.keys must haveLength(2)
          responseAs[NextTrialsResponse].explicits.sortBy(byFirst) must_== expected.explicits.sortBy(byFirst)
        }
      }

      "reject anything other than GET requests" in {

        Post(String.format(nextFacepuzzlePath, EncodedId(currentSession.id).encoded))
          .withHeaders(Authorization(OAuth2BearerToken(token))) ~> facePuzzleApi.makeRoutes(prefix, modulePath, impl) ~> check {
          rejections === List(MethodRejection(HttpMethods.GET))
        }

        Patch(String.format(nextFacepuzzlePath, EncodedId(currentSession.id).encoded))
          .withHeaders(Authorization(OAuth2BearerToken(token))) ~> facePuzzleApi.makeRoutes(prefix, modulePath, impl) ~> check {
          rejections === List(MethodRejection(HttpMethods.GET))
        }

        Put(String.format(nextFacepuzzlePath, EncodedId(currentSession.id).encoded))
          .withHeaders(Authorization(OAuth2BearerToken(token))) ~> facePuzzleApi.makeRoutes(prefix, modulePath, impl) ~> check {
          rejections === List(MethodRejection(HttpMethods.GET))
        }

        Options(String.format(nextFacepuzzlePath, EncodedId(currentSession.id).encoded))
          .withHeaders(Authorization(OAuth2BearerToken(token))) ~> facePuzzleApi.makeRoutes(prefix, modulePath, impl) ~> check {
          rejections === List(MethodRejection(HttpMethods.GET))
        }

        Delete(String.format(nextFacepuzzlePath, EncodedId(currentSession.id).encoded))
          .withHeaders(Authorization(OAuth2BearerToken(token))) ~> facePuzzleApi.makeRoutes(prefix, modulePath, impl) ~> check {
          rejections === List(MethodRejection(HttpMethods.GET))
        }

        Head(String.format(nextFacepuzzlePath, EncodedId(currentSession.id).encoded))
          .withHeaders(Authorization(OAuth2BearerToken(token))) ~> facePuzzleApi.makeRoutes(prefix, modulePath, impl) ~> check {
          rejections === List(MethodRejection(HttpMethods.GET))
        }

      }
    }

    f"/$prefix/$modulePath/<id>/check" should {

      "return checked assetIds" in {

        Post(String.format(checkFacepuzzlePath, EncodedId(currentSession.id).encoded))
          .withHeaders(Authorization(new OAuth2BearerToken(token)))
          .withEntity(HttpEntity(ContentTypes.`application/json`, s"""{
              "trialId": "${EncodedId(trialObjects(0)._1.id).encoded}",
              "assetIds": [${(matchingTrials map ('"' + EncodedId(_).encoded + '"')).mkString(",")}]
            }""")) ~> facePuzzleApi.makeRoutes(prefix, modulePath, impl) ~> check {

          val expected = CheckGameResultResponse(Some(AssetResponseUtil.fromAsset(completed)), matchingTrials map (EncodedId(_)), true)
          val byId = { a: EncodedId => a.id }

          responseAs[CheckGameResultResponse].matching must beTrue
          responseAs[CheckGameResultResponse].assetIds.sortBy(byId) must_== expected.assetIds.sortBy(byId)
        }

        val notMatching = List(EncodedId(5), EncodedId(4))
        Post(String.format(checkFacepuzzlePath, EncodedId(currentSession.id).encoded))
          .withHeaders(Authorization(new OAuth2BearerToken(token)))
          .withEntity(HttpEntity(ContentTypes.`application/json`, s"""{
              "trialId": "${EncodedId(trialObjects(0)._1.id).encoded}",
              "assetIds":[${(notMatching.map(n => '"' + n.encoded + '"')).mkString(",")}]
            }""")) ~> facePuzzleApi.makeRoutes(prefix, modulePath, impl) ~> check {

          val byId = { a: EncodedId => a.id }

          responseAs[CheckGameResultResponse].matching must beFalse
          responseAs[CheckGameResultResponse].assetIds.sortBy(byId) must_== notMatching.sortBy(byId)
        }
      }


      "reject invalid/unauthorized session ids" in {

        Post(String.format(checkFacepuzzlePath, EncodedId(currentSession.id).encoded))
          .withHeaders(Authorization(new OAuth2BearerToken(token)))
          .withEntity(HttpEntity(ContentTypes.`application/json`, s"""{
              "trialId": "${EncodedId(trialObjects(0)._1.id).encoded}",
              "assetIds": [${(matchingTrials map ('"' + EncodedId(_).encoded + '"')).mkString(",")}]
            }""")) ~> facePuzzleApi.makeRoutes(prefix, modulePath, impl) ~> check {

          val expected = CheckGameResultResponse(Some(AssetResponseUtil.fromAsset(completed)), matchingTrials map (EncodedId(_)), true)
          val byId = { a: EncodedId => a.id }

          responseAs[CheckGameResultResponse].matching must beTrue
          responseAs[CheckGameResultResponse].assetIds.sortBy(byId) must_== expected.assetIds.sortBy(byId)
        }

        Post(String.format(checkFacepuzzlePath, EncodedId(currentSession.id).encoded))
          .withHeaders(Authorization(new OAuth2BearerToken(otherToken)))
          .withEntity(HttpEntity(ContentTypes.`application/json`, s"""{
              "trialId": "${EncodedId(trialObjects(0)._1.id).encoded}",
              "assetIds": [${(matchingTrials map ('"' + EncodedId(_).encoded + '"')).mkString(",")}]
            }""")) ~> facePuzzleApi.makeRoutes(prefix, modulePath, impl) ~> check {
          rejections === List(AuthenticationFailedRejection.apply(CredentialsRejected, List()))
        }
      }

      "return the fully completed asset on match" in {

        Post(String.format(checkFacepuzzlePath, EncodedId(currentSession.id).encoded))
          .withHeaders(Authorization(new OAuth2BearerToken(token)))
          .withEntity(HttpEntity(ContentTypes.`application/json`, s"""{
              "trialId": "${EncodedId(trialObjects(0)._1.id).encoded}",
              "assetIds": [${(matchingTrials map ('"' + EncodedId(_).encoded + '"')).mkString(",")}]
            }""")) ~> facePuzzleApi.makeRoutes(prefix, modulePath, impl) ~> check {
          val emotions = (trialObjects map (t => (EncodedId(t._1.id), allEmotions(t._1.id).name)) take trials).toSeq ++ List((EncodedId(trials + 1), emotion.name))

          val expected = CheckGameResultResponse(Some(AssetResponseUtil.fromAsset(completed)), matchingTrials map (EncodedId(_)), true)
          val byId = { a: EncodedId => a.id }

          responseAs[CheckGameResultResponse].matching must beTrue
          responseAs[CheckGameResultResponse].assetIds.sortBy(byId) must_== expected.assetIds.sortBy(byId)
          responseAs[CheckGameResultResponse].completedAsset must_== expected.completedAsset
        }
      }


      "reject incompatible assets" in {
        val notMatching = List(EncodedId(5), EncodedId(4))
        Post(String.format(checkFacepuzzlePath, EncodedId(currentSession.id).encoded))
          .withHeaders(Authorization(new OAuth2BearerToken(token)))
          .withEntity(HttpEntity(ContentTypes.`application/json`, s"""{
              "trialId": "${EncodedId(trialObjects(0)._1.id).encoded}",
              "assetIds": [${(notMatching.map(n => '"' + n.encoded + '"')).mkString(",")}]
            }""")) ~> facePuzzleApi.makeRoutes(prefix, modulePath, impl) ~> check {

          val byId = { a: EncodedId => a.id }

          responseAs[CheckGameResultResponse].matching must beFalse
          responseAs[CheckGameResultResponse].assetIds.sortBy(byId) must_== notMatching.sortBy(byId)
        }
      }

      "reject invalid/unauthorized trial ids" in {

        val invalidId = EncodedId(123L)
        Post(String.format(checkFacepuzzlePath, EncodedId(currentSession.id).encoded))
          .withHeaders(Authorization(new OAuth2BearerToken(token)))
          .withEntity(HttpEntity(ContentTypes.`application/json`, s"""{
              "trialId": "${invalidId.encoded}",
              "assetIds": [${(matchingTrials map ('"' + EncodedId(_).encoded + '"')).mkString(",")}]
            }""")) ~> facePuzzleApi.makeRoutes(prefix, modulePath, impl) ~> check {
          rejections === List(MalformedRequestContentRejection(invalidId.encoded))
        }

        Post(String.format(checkFacepuzzlePath, EncodedId(currentSession.id).encoded))
          .withHeaders(Authorization(new OAuth2BearerToken(token)))
          .withEntity(HttpEntity(ContentTypes.`application/json`, s"""{
              "trialId": "hello",
              "assetIds": [${(matchingTrials map ('"' + EncodedId(_).encoded + '"')).mkString(",")}]
            }""")) ~> facePuzzleApi.makeRoutes(prefix, modulePath, impl) ~> check {
          rejection.isInstanceOf[MalformedRequestContentRejection] must beTrue
        }

        Post(String.format(checkFacepuzzlePath, EncodedId(currentSession.id).encoded))
          .withHeaders(Authorization(new OAuth2BearerToken(token)))
          .withEntity(HttpEntity(ContentTypes.`application/json`, s"""{
              "trialId": 123,
              "assetIds": [${(matchingTrials map ('"' + EncodedId(_).encoded + '"')).mkString(",")}]
            }""")) ~> facePuzzleApi.makeRoutes(prefix, modulePath, impl) ~> check {
          rejection.isInstanceOf[MalformedRequestContentRejection] must beTrue

        }

        Post(String.format(checkFacepuzzlePath, EncodedId(currentSession.id).encoded))
          .withHeaders(Authorization(new OAuth2BearerToken(token)))
          .withEntity(HttpEntity(ContentTypes.`application/json`, s"""{
              "trialId": ".'#!%<$$<<f=≠}(öäü*~",
              "assetIds": [${(matchingTrials map ('"' + EncodedId(_).encoded + '"')).mkString(",")}]
            }""")) ~> facePuzzleApi.makeRoutes(prefix, modulePath, impl) ~> check {
          rejection.isInstanceOf[MalformedRequestContentRejection] must beTrue
        }
      }

      "reject when no body is sent" in {

        Post(String.format(checkFacepuzzlePath, EncodedId(currentSession.id).encoded))
          .withHeaders(Authorization(OAuth2BearerToken(token))) ~> facePuzzleApi.makeRoutes(prefix, modulePath, impl) ~> check {
          rejections === List(RequestEntityExpectedRejection)
        }
      }

      "reject content types other than 'application/json'" in {

        Post(String.format(checkFacepuzzlePath, EncodedId(currentSession.id).encoded))
          .withHeaders(Authorization(OAuth2BearerToken(token)))
          .withEntity(HttpEntity(ContentTypes.`text/plain(UTF-8)`, s"""{
              "trialId": "${EncodedId(trialObjects(0)._1.id).encoded}",
              "assetIds": [${(matchingTrials map ('"' + EncodedId(_).encoded + '"')).mkString(",")}]
            }""")) ~> facePuzzleApi.makeRoutes(prefix, modulePath, impl) ~> check {
          rejections === List(UnsupportedRequestContentTypeRejection("Expected 'application/json'"))
        }

        Post(String.format(checkFacepuzzlePath, EncodedId(currentSession.id).encoded))
          .withHeaders(Authorization(OAuth2BearerToken(token)))
          .withEntity(HttpEntity(ContentTypes.NoContentType, s"""{
              "trialId": "${EncodedId(trialObjects(0)._1.id).encoded}",
              "assetIds": [${(matchingTrials map ('"' + EncodedId(_).encoded + '"')).mkString(",")}]
            }""")) ~> facePuzzleApi.makeRoutes(prefix, modulePath, impl) ~> check {
          rejections === List(UnsupportedRequestContentTypeRejection("Expected 'application/json'"))
        }

        Post(String.format(checkFacepuzzlePath, EncodedId(currentSession.id).encoded))
          .withHeaders(Authorization(OAuth2BearerToken(token)))
          .withEntity(HttpEntity(ContentTypes.`application/octet-stream`, s"""{
              "trialId": "${EncodedId(trialObjects(0)._1.id).encoded}",
              "assetIds": [${(matchingTrials map ('"' + EncodedId(_).encoded + '"')).mkString(",")}]
            }""")) ~> facePuzzleApi.makeRoutes(prefix, modulePath, impl) ~> check {
          rejections === List(UnsupportedRequestContentTypeRejection("Expected 'application/json'"))
        }
      }

      "reject anything other than POST requests" in {

        Get(String.format(checkFacepuzzlePath, EncodedId(currentSession.id).encoded))
          .withHeaders(Authorization(OAuth2BearerToken(token))) ~> facePuzzleApi.makeRoutes(prefix, modulePath, impl) ~> check {
          rejections === List(MethodRejection(HttpMethods.POST))
        }

        Patch(String.format(checkFacepuzzlePath, EncodedId(currentSession.id).encoded))
          .withHeaders(Authorization(OAuth2BearerToken(token))) ~> facePuzzleApi.makeRoutes(prefix, modulePath, impl) ~> check {
          rejections === List(MethodRejection(HttpMethods.POST))
        }

        Put(String.format(checkFacepuzzlePath, EncodedId(currentSession.id).encoded))
          .withHeaders(Authorization(OAuth2BearerToken(token))) ~> facePuzzleApi.makeRoutes(prefix, modulePath, impl) ~> check {
          rejections === List(MethodRejection(HttpMethods.POST))
        }

        Options(String.format(checkFacepuzzlePath, EncodedId(currentSession.id).encoded))
          .withHeaders(Authorization(OAuth2BearerToken(token))) ~> facePuzzleApi.makeRoutes(prefix, modulePath, impl) ~> check {
          rejections === List(MethodRejection(HttpMethods.POST))
        }

        Delete(String.format(checkFacepuzzlePath, EncodedId(currentSession.id).encoded))
          .withHeaders(Authorization(OAuth2BearerToken(token))) ~> facePuzzleApi.makeRoutes(prefix, modulePath, impl) ~> check {
          rejections === List(MethodRejection(HttpMethods.POST))
        }

        Head(String.format(checkFacepuzzlePath, EncodedId(currentSession.id).encoded))
          .withHeaders(Authorization(OAuth2BearerToken(token))) ~> facePuzzleApi.makeRoutes(prefix, modulePath, impl) ~> check {
          rejections === List(MethodRejection(HttpMethods.POST))
        }

      }
    }
  }
}