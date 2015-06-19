package de.afs.platform.dao

import de.afs.platform.dao.db.slick.Driver.simple._
import de.afs.platform.dao.db.slick._
import de.afs.platform.dao.db.slick.tables.{Users, ModuleSessions, Modules}
import de.afs.platform.domain._
import de.afs.platform.test.utils.InMemoryTestDatabase
import org.h2.jdbc.JdbcSQLException
import org.specs2.execute.{AsResult, Result}
import org.specs2.mutable._

/**
 *
 */
class ModuleSessionDAOSpec extends Specification with InMemoryTestDatabase {

  def hasModuleSessionTable(implicit s: Session) = {
    val tbls = s.metaData.getTables(null, null, "%", null)
    var tableNames = List[String]()
    while (tbls.next) {
      tableNames = tableNames ::: List(tbls.getString("TABLE_NAME"))
    }

    tableNames.exists(_ == ModuleSessions.table.baseTableRow.tableName)
  }


  val module = new Module(1, "module")
  val trials = 1
  val difficulty = 1
  val length = 1
  val user = new User(1, "a@b.com", "password", false, None)

  "The ModuleSession DAO" should {
    "have a method createTable that" should {

      "create the table" in anEmptyDatabase() {
        (db, dao, session) => implicit val s: Session = session

          dao.createTable()

          hasModuleSessionTable must beTrue

      }
    }

    "have a method dropTable that" should {

      "drop the table" in anEmptyDatabase() {
        (db, dao, session) => implicit val s: Session = session

          dao.createTable()
          hasModuleSessionTable must beTrue

          dao.dropTable() must not(throwA[JdbcSQLException])

      }

    }

    "have a method getById that" should {

      "return the correct object for an id" in aDatabaseWithData() {
        (unsaved, saved, dao, session) => implicit val s: Session = session
          dao.getById(saved.id) must beSome(saved)
      }

      "return None if an id was not found" in aDatabaseWithData() {
        (unsaved, saved, dao, session) => implicit val s: Session = session
          dao.getById(123) must beNone
      }
    }


    "have a method save that" should {

      "correctly persist updates to an object" in aDatabaseWithData() {
        (unsaved, saved, dao, session) => implicit val s: Session = session
          dao.save(unsaved) must_== saved.copy(id = 2)
          dao.getById(saved.id) must beSome(saved)
      }

      "create a new database entry if an object was not previously persisted" in aDatabaseWithData() {
        (unsaved, saved, dao, session) => implicit val s: Session = session
          val newObj = dao.save(unsaved)
          newObj.id != saved.id must beTrue

          dao.getById(newObj.id) must beSome(newObj)
      }

      "increment the id of a newly persisted object" in aDatabaseWithData() {
        (unsaved, saved, dao, session) => implicit val s: Session = session

          val newObj = dao.save(unsaved)
          newObj.id != saved.id must beTrue

          newObj.id must be_>(saved.id)
      }
    }

    "have a method delete that" should {

      "correctly delete an object" in aDatabaseWithData() {
        (unsaved, saved, dao, session) => implicit val s: Session = session
          dao.delete(saved) must_== unsaved

          dao.getById(saved.id) must beNone
      }

      "set the id to 0 on deletion" in aDatabaseWithData() {
        (unsaved, saved, dao, session) => implicit val s: Session = session
          dao.delete(saved).id must_== 0
      }

      "do nothing if an object is not in the database" in aDatabaseWithData() {
        (unsaved, saved, dao, session) => implicit val s: Session = session

          dao.delete(unsaved) must_== unsaved
      }
    }

    "have a method joinUsersAndModules that" should {

      "retrieve the associated objects" in aDatabaseWithData() {
        (unsaved, saved, dao, session) => implicit val s: Session = session

          dao.joinUsersAndModules(saved) must beSome(UserModuleSessionWithModule(saved, user, module))
      }

      "return None if an invalid session is passed in" in aDatabaseWithData() {
        (unsaved, saved, dao, session) => implicit val s: Session = session
          dao.joinUsersAndModules(unsaved) must beNone
      }
    }

  }

  def anEmptyDatabase[T]()(test: (Database, ModuleSessionDAO[Session], Session) => T)(implicit evidence$1: AsResult[T]): Result = {
    var result: T = null.asInstanceOf[T]


    val db = Database.forURL(URL, driver = Driver)


    val moduleDaoC = new SlickModuleSessionDAOComponent with SlickDatabaseHandle {
      database = db
    }

    db.withSession {
      implicit s: Session =>
        (Users.table.ddl ++ Modules.table.ddl).create

        result = test(db, moduleDaoC.moduleSessionDao, s)
    }
    AsResult[T](result)
  }

  def aDatabaseWithData[T]()(test: (ModuleSession, ModuleSession, ModuleSessionDAO[Session], Session) => T)(implicit evidence$1: AsResult[T]): Result = {
    var result: T = null.asInstanceOf[T]

    val table = ModuleSessions.table

    val db = Database.forURL(URL, driver = Driver)

    val moduleSessionDaoC = new SlickModuleSessionDAOComponent with SlickDatabaseHandle {
      database = db
    }

    db.withSession {
      implicit s: Session =>

        (Users.table.ddl ++ table.ddl ++ Modules.table.ddl).create

        Modules.table.insert(module)
        Users.table.insert(user)


        val unsaved = new ModuleSession(0, module.id, user.id, difficulty, trials, length)
        val saved = unsaved.copy(id = 1)

        table.insert(unsaved)


        result = test(unsaved, saved, moduleSessionDaoC.moduleSessionDao, s)
        table.ddl.drop
    }
    AsResult[T](result)
  }

}
