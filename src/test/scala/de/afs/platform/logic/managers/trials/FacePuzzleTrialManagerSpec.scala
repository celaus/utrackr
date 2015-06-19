package de.afs.platform.logic.managers.trials

import de.afs.platform.dao._
import de.afs.platform.dao.db.slick._
import de.afs.platform.domain._
import de.afs.platform.logic.TrialManager
import de.afs.platform.test.utils.InMemoryTestDatabase
import org.specs2.execute.{AsResult, Result}
import org.specs2.mock.Mockito
import org.specs2.mutable._

import de.afs.platform.dao.db.slick.Driver.simple.Session

import scala.slick.driver.H2Driver.simple._


class FacePuzzleTrialManagerSpec extends Specification with Mockito {

  val user = User(1, "mail", "pwd", false, None)
  val completed = Asset(3, "mime", "loc", 0, None, None)
  val module = Module(1, "module")
  val item: Item = Item(1, 2, module.id, Some(completed.id), 1)
  val assets = List(Asset(1, "mime", "loc", 0, Some(item.id), None),
    Asset(2, "mime", "loc", 0, Some(item.id), None))

  val session = ModuleSession(1, module.id, user.id, 1, 1, 2)
  val finishedSession = ModuleSession(1, module.id, user.id, 1, 1, 1)

  val trial = TrialData(1, 1, item.id, session.id)
  val anotherSession = ModuleSession(2, module.id, user.id, 1, 1, 2)

  "A FacePuzzleTrialManager" should {

    /*"have a method save that" should {

      "save and returns an object with the id set" in aDatabaseWithoutData() { (unsaved, saved, manager) =>
        manager.save(unsaved) must_== (saved)
      }

      "save and correctly increments the assigned id" in aDatabaseWithoutData() { (unsaved, saved, manager) =>
        val anotherSavedUser = saved.copy(id = 2)
        manager.save(unsaved) must_== (saved)
        manager.save(unsaved) must_== (anotherSavedUser)
      }

      "update users if persistent" in aDatabaseWithData() { (unsaved, saved, manager) =>
        val newEmail = "a@b.com"
        saved.email = newEmail
        manager.save(saved) must_== (saved)
        val savedFromDB = manager.findUserById(saved.id)
        savedFromDB must beSome(saved)

        savedFromDB.get.email must_== (newEmail)
      }
    }


    "have a method delete that" should {
      "delete a previously saved user" in aDatabaseWithData() { (unsaved, saved, manager) =>
        manager.findUserById(1) must beSome(saved)
        manager.delete(saved) must_== (unsaved)
        manager.findUserById(1) must beNone
      }

    }   */

    "have a method nextTrials that" should {
      "return Trials and Assets according to the session's settings" in aDatabaseWithData() { manager =>
        manager nextTrials (session) must containAllOf(List((trial.copy(id = 2, ordering = 2), assets)))
      }

      "return Trials and Assets until a configured number of trials has been reached" in aDatabaseWithData() { manager =>
        manager nextTrials (finishedSession) must beEmpty
      }
    }

    "have a method trySolve that" should {
      "find and return the common Item for several Assets if possible" in aDatabaseWithData() { manager =>
        manager trySolve(trial, assets map (_.id)) must beSome(ItemWithCompletedAsset(item, completed))
      }

      "return None if no common Item for several Assets is found" in aDatabaseWithData() { manager =>
        manager trySolve(trial, List()) must beNone
        manager trySolve(trial, List(123, 123)) must beNone
        manager trySolve(trial, (assets map (_.id) take 1) ++ List(123)) must beNone
      }
    }


    "have a method trialDataForId that" should {

      "return the corresponding TrialData from within a session" in aDatabaseWithData() { manager =>
        manager.trialDataForId(session, trial.id) must beSome(trial)
      }

      "return None if the trial is not present within a session" in aDatabaseWithData() { manager =>
        manager.trialDataForId(session, 123) must beNone
        manager.trialDataForId(anotherSession, trial.id) must beNone
      }
    }
  }

  private def managerComponent = new DBFacePuzzleTrialManagerComponent
    with ModuleSessionDAOComponent[Session]
    with AssetDAOComponent[Session]
    with ItemDAOComponent[Session]
    with TrialDataDAOComponent[Session]
    with SlickDatabaseHandle {
    implicit val s: Session = mock[Session]

    database = Database.forURL(new InMemoryTestDatabase {}.URL, driver = new InMemoryTestDatabase {}.Driver)


    override def moduleSessionDao = {
      val m = mock[ModuleSessionDAOInterface]
        .extraInterface[BaseDAO[ModuleSession, Session]]
        .smart
      m.joinUsersAndModules(===(session))(any) returns Some(UserModuleSessionWithModule(session, user, module))
      m
    }


    override def assetDao = {
      val m = mock[AssetDAOInterface]
        .extraInterface[BaseDAO[Asset, Session]]
        .smart
      m.defaultReturn(List())
      m.forIds(===(Set(1, 2)))(any) returns assets
      m
    }

    override def itemDao = {
      val m = mock[ItemDAOInterface]
        .extraInterface[BaseDAO[Item, Session]]
        .smart

      m.fromParts(===(assets), any)(any) returns Some(item)
      m.fromParts(===(List()), any)(any) returns None
      m.completedAssetForItem(===(item))(any) returns Some(ItemWithCompletedAsset(item, completed))
      m.itemsFor(===(module), anyInt, any)(any) returns List(item)
      m.partsOfItem(===(item), any)(any) returns ItemWithParts(item, assets)
      m
    }


    override def trialDataDAO = {
      val m = mock[TrialDataDAOInterface]
        .extraInterface[BaseDAO[TrialData, Session]]

      m.save(any[TrialData])(any) answers { t => (t.asInstanceOf[TrialData]).copy(id = 2) }
      m.getById(===(trial.id))(any) returns Some(trial)
      m.trialsForSession(===(session))(any) returns List(trial)
      m.trialsForSession(===(finishedSession))(any) returns List(trial)
      m
    }
  }


  def aDatabaseWithData[T]()(test: (TrialManager) => T)(implicit evidence$1: AsResult[T]): Result = AsResult[T](test(managerComponent.trialManager))
}
