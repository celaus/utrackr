package de.afs.platform.test.utils

import com.github.nscala_time.time.Imports._
import de.afs.platform.common.config.AuthenticationConfiguration
import de.afs.platform.domain._
import de.afs.platform.logic._
import de.afs.platform.logic.managers.JWTokenManagerComponent


/**
 *
 */
object ManagerCreator {

  def createAuthenticationConfig(tokenSecret: String, algorithm: String, expiration: Duration) = new AuthenticationConfiguration {

    override def secret: String = tokenSecret

    override def algorithmName: String = algorithm

    override def tokenExpiration: Duration = expiration

  }

  def createTokenManager(tokenSecret: String, algorithm: String, expiration: Duration) = new JWTokenManagerComponent with AuthenticationConfiguration {

    override def secret: String = tokenSecret

    override def algorithmName: String = algorithm

    override def tokenExpiration: Duration = expiration

  }

  def createModuleManager(module: Module) = new ModuleManager {

    override def byId(moduleId: Int) = if (moduleId == module.id) Some(module) else None

    override def saveModule(module: Module) = module copy (id = module.id + 1)
  }

  def createUserManager(aUser: User, newToken: String, users: List[User] = List()) = new UserManager {

    val userRep = if (users.isEmpty) List(aUser) else users

    override def save(user: User): User = user

    override def findUserById(id: Integer): Option[User] = users.find(id == _.id)

    override def findUserByMailAndPassword(email: String, password: String): Option[User] =
      users.find(u => (u.email == email) && (u.password == password))

    override def findUserByToken(token: String): Option[User] = users.find(token == _.token)

    override def delete(user: User): User = user.copy(id = 0)

    override def resetPasswordOf(user: User): User =
      user.copy(passwordReset = true, password = "", token = Some(newToken))

    override def findUserByMail(email: String): Option[User] = users.find(email == _.email)
  }

  def createModuleSessionManager(currentSession: ModuleSession, trialEmotion: Emotion) = new ModuleSessionManager {

    override def byId(sessionId: Int) =
      if (currentSession.id == sessionId) Some(currentSession) else None

    override def createSession(module: Module, user: User, options: ModuleSessionOptions) = Some(ModuleSession(1, module.id, user.id, options.difficulty, options.trials, options.sessionLength))


    override def saveItem(item: Item) = item.copy(id = item.id + 1)


    override def saveAsset(asset: Asset) = asset copy (id = asset.id + 1)
  }


  def createTrialManager(currentSession: ModuleSession, trials: Seq[(TrialData, Seq[Asset])], fittingParts: Seq[Int], completedTrial: ItemWithCompletedAsset) = new TrialManager {

    override def trialDataForId(moduleSession: ModuleSession, trialId: Int) = trials map (_._1) find (t => t.id == trialId && t.moduleSessionId == moduleSession.id)


    override def trySolve(trial: TrialData, parts: Seq[Int]) = if (parts == fittingParts) Some(completedTrial) else None


    override def nextTrials(session: ModuleSession) = if (currentSession == session) trials take session.trials else List()
  }

  def createEmotionManager(allEmotions: List[Emotion]) = new EmotionManager {

    override def byId(emotionId: Int) = allEmotions find (_.id == emotionId)

    override def saveEmotion(emotion: Emotion) =
      emotion.copy(id = emotion.id + 1)


    override def emotionForTrial(trial: TrialData) = if (trial.id > 0) Some(allEmotions(trial.id)) else None

    override def getEmotions(n: Int, includeWhen: (Emotion) => Boolean) = allEmotions filter includeWhen take n


  }

}
