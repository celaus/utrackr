package de.afs.platform.logic.managers

import org.specs2.mutable.Specification
import spray.http.HttpHeaders.Authorization
import spray.http.{ContentTypes, HttpEntity, OAuth2BearerToken}
import spray.routing.MalformedRequestContentRejection

/**
 * Created by cm on 16/04/15.
 */     /*
class ModuleSessionManagerSpec extends Specification {
 "A ModuleSessionManager" should {
    "reject invalid module option values" in {

      Post(moduleFacepuzzlePath)
        .withHeaders(Authorization(OAuth2BearerToken(token)))
        .withEntity(HttpEntity(ContentTypes.`application/json`, s"""{
              "session": 100000000000,
              "trials": 2,
              "difficulty": 5
            }""")) ~> facePuzzleApi.makeRoutes(prefix, modulePath) ~> check {
        rejections must haveLength(1)
        rejections(0).isInstanceOf[MalformedRequestContentRejection] must beTrue
      }

      Post(moduleFacepuzzlePath)
        .withHeaders(Authorization(OAuth2BearerToken(token)))
        .withEntity(HttpEntity(ContentTypes.`application/json`, s"""{
              "session": 5,
              "trials": 2000000000,
              "difficulty": 5
            }""")) ~> facePuzzleApi.makeRoutes(prefix, modulePath) ~> check {
        rejections must haveLength(1)
        rejections(0).isInstanceOf[MalformedRequestContentRejection] must beTrue
      }

      Post(moduleFacepuzzlePath)
        .withHeaders(Authorization(OAuth2BearerToken(token)))
        .withEntity(HttpEntity(ContentTypes.`application/json`, s"""{
              "session": 5,
              "trials": 2,
              "difficulty": 5000203
            }""")) ~> facePuzzleApi.makeRoutes(prefix, modulePath) ~> check {
        rejections must haveLength(1)
        rejections(0).isInstanceOf[MalformedRequestContentRejection] must beTrue
      }

      Post(moduleFacepuzzlePath)
        .withHeaders(Authorization(OAuth2BearerToken(token)))
        .withEntity(HttpEntity(ContentTypes.`application/json`, s"""{
              "session": -1304,
              "trials": 0,
              "difficulty": 5
            }""")) ~> facePuzzleApi.makeRoutes(prefix, modulePath) ~> check {
        rejections must haveLength(1)
        rejections(0).isInstanceOf[MalformedRequestContentRejection] must beTrue
      }

      Post(moduleFacepuzzlePath)
        .withHeaders(Authorization(OAuth2BearerToken(token)))
        .withEntity(HttpEntity(ContentTypes.`application/json`, s"""{
              "session": 100000000000,
              "trials": 2,
              "difficulty": 5
            }""")) ~> facePuzzleApi.makeRoutes(prefix, modulePath) ~> check {
        rejections must haveLength(1)
        rejections(0).isInstanceOf[MalformedRequestContentRejection] must beTrue
      }
    }
  }

}         */
