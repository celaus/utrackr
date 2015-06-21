package de.cm.utrackr.service.web

import akka.actor.ActorRefFactory
import com.typesafe.scalalogging.LazyLogging
import spray.routing.HttpService._

trait AppServiceApi extends LazyLogging {

  val indexPagePath: String
  val resourceDirectoryPath: String


  implicit def actorRefFactory: ActorRefFactory

  val appServiceRoute =
    pathSingleSlash {
      getFromResource(indexPagePath)
    } ~ {
      getFromResourceDirectory(resourceDirectoryPath)
    }
}

