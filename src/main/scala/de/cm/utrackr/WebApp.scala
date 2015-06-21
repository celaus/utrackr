package de.cm.utrackr

import akka.actor.{ActorSystem, Props}
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import com.github.nscala_time.time.Imports._
import de.cm.utrackr.common.config.{AppConfig, AuthenticationConfiguration}
import de.cm.utrackr.service.web.WebAppActor
import spray.can.Http


/**
 * The main class to start the server.
 */
object WebApp extends App with AppConfig.WebApp {

  // we need an ActorSystem to host our application in
  implicit val system = ActorSystem("on-spray-can")
  val algorithm = "HS256"
  val tokenSecret = "secret"
  val expiration = 2.days.standardDuration


  /*val userManagerComponent = new DBUserManagerComponent
    with SlickUserDAOComponent
    with SlickDatabaseHandle {
    database = databaseHandle
  }     */


  /*if (createTables) databaseHandle.withSession {  =>
    userManagerComponent.userDao.createTable()
    moduleManagerComponent.moduleDao.createTable()
    emotionManagerComponent.emotionDao.createTable()
    moduleSessionManagerComponent.moduleSessionDao.createTable()
    moduleSessionManagerComponent.itemDao.createTable()
    moduleSessionManagerComponent.assetDao.createTable()
    facePuzzleTrialManagerComponent.trialDataDAO.createTable()
  }     */

  val authConfig = new AuthenticationConfiguration {
    override def algorithmName: String = "HS256"

    override def secret: String = tokenSecret

    override def tokenExpiration: Duration = expiration
  }

  /*val tokenManagerComponent = new JWTokenManagerComponent with AuthenticationConfiguration /* with AppConfig.Authentication*/ {
    override def algorithmName: String = "HS256"

    override def secret: String = tokenSecret

    override def tokenExpiration: Duration = expiration
  } */

  // create and start our service actor
  val service = system.actorOf(Props(classOf[WebAppActor],
    authConfig),
    "webservices")

  implicit val timeout = Timeout.longToTimeout(5.seconds.millis)
  // start a new HTTP server on port 8080 with our service actor as the handler
  IO(Http) ? Http.Bind(service, interface = interface, port = port)
}
