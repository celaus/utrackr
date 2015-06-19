package de.cm.utrackr

import akka.actor.{ActorSystem, Props}
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import com.github.nscala_time.time.Imports._
import com.mchange.v2.c3p0.ComboPooledDataSource
import de.afs.platform.common.config.{AppConfig, AuthenticationConfiguration}
import de.afs.platform.dao.db.slick.Driver.simple._
import de.afs.platform.dao.db.slick._
import de.afs.platform.logic.managers._
import de.afs.platform.logic.managers.trials.{DBFacePuzzleTrialManagerComponent, DBFilmPuzzleTrialManagerComponent, DBWhoSpeaksTrialManagerComponent}
import de.afs.platform.service.web.WebAppActor
import spray.can.Http


/**
 * The main class to start the server.
 */
object WebApp extends App with AppConfig.WebApp {
  val facepuzzleModuleId = 1
  val whospeaksModuleId = 2
  val filmpuzzleModuleId = 3

  // we need an ActorSystem to host our application in
  implicit val system = ActorSystem("on-spray-can")
  val algorithm = "HS256"
  val tokenSecret = "5db67d09559e451d86b8a80faa1f595ef11ea4c8247b66153b480dca0289fb31"
  val expiration = 2.days.standardDuration

  val databaseHandle = Database.forDataSource(new ComboPooledDataSource)

  val userManagerComponent = new DBUserManagerComponent
    with SlickUserDAOComponent
    with SlickDatabaseHandle {
    database = databaseHandle
  }

  val moduleSessionManagerComponent = new DBModuleSessionManagerComponent
    with SlickModuleSessionDAOComponent
    with SlickItemDAOComponent
    with SlickAssetDAOComponent
    with SlickDatabaseHandle {
    database = databaseHandle
  }
  val moduleManagerComponent = new DBModuleManagerComponent
    with SlickModuleDAOComponent
    with SlickDatabaseHandle {
    database = databaseHandle
  }

  val emotionManagerComponent = new DBEmotionManagerComponent
    with SlickItemDAOComponent
    with SlickEmotionDAOComponent
    with SlickDatabaseHandle {
    database = databaseHandle
  }

  val facePuzzleTrialManagerComponent = new DBFacePuzzleTrialManagerComponent
    with SlickModuleSessionDAOComponent
    with SlickAssetDAOComponent
    with SlickItemDAOComponent
    with SlickTrialDataDAOComponent
    with SlickDatabaseHandle {
    database = databaseHandle
  }

  val filmPuzzleTrialManagerComponent = new DBFilmPuzzleTrialManagerComponent
    with SlickModuleSessionDAOComponent
    with SlickAssetDAOComponent
    with SlickItemDAOComponent
    with SlickTrialDataDAOComponent
    with SlickDatabaseHandle {
    database = databaseHandle
  }

  val whoSpeaksTrialManagerComponent = new DBWhoSpeaksTrialManagerComponent
    with SlickModuleSessionDAOComponent
    with SlickAssetDAOComponent
    with SlickItemDAOComponent
    with SlickTrialDataDAOComponent
    with SlickDatabaseHandle {
    database = databaseHandle
  }


  if (createTables) databaseHandle.withSession { implicit s: Session =>
    userManagerComponent.userDao.createTable()
    moduleManagerComponent.moduleDao.createTable()
    emotionManagerComponent.emotionDao.createTable()
    moduleSessionManagerComponent.moduleSessionDao.createTable()
    moduleSessionManagerComponent.itemDao.createTable()
    moduleSessionManagerComponent.assetDao.createTable()
    facePuzzleTrialManagerComponent.trialDataDAO.createTable()
  }

  val authConfig = new AuthenticationConfiguration {
    override def algorithmName: String = "HS256"

    override def secret: String = tokenSecret

    override def tokenExpiration: Duration = expiration
  }

  val tokenManagerComponent = new JWTokenManagerComponent with AuthenticationConfiguration /* with AppConfig.Authentication*/ {
    override def algorithmName: String = "HS256"

    override def secret: String = tokenSecret

    override def tokenExpiration: Duration = expiration
  }

  // create and start our service actor
  val service = system.actorOf(Props(classOf[WebAppActor],
    userManagerComponent.userManager,
    authConfig,
    tokenManagerComponent.tokenManager,
    moduleSessionManagerComponent.moduleSessionManager,
    moduleManagerComponent.moduleManager,
    emotionManagerComponent.emotionManager,
    "module",
    moduleManagerComponent.moduleManager.byId(facepuzzleModuleId).get,
    facePuzzleTrialManagerComponent.trialManager,
    moduleManagerComponent.moduleManager.byId(whospeaksModuleId).get,
    whoSpeaksTrialManagerComponent.trialManager,
    moduleManagerComponent.moduleManager.byId(filmpuzzleModuleId).get,
    filmPuzzleTrialManagerComponent.trialManager),
    "webservices")

  implicit val timeout = Timeout.longToTimeout(5.seconds.millis)
  // start a new HTTP server on port 8080 with our service actor as the handler
  IO(Http) ? Http.Bind(service, interface = interface, port = port)
}
