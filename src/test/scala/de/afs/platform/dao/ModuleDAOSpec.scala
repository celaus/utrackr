package de.afs.platform.dao


import de.afs.platform.dao.db.slick.Driver.simple._
import de.afs.platform.dao.db.slick._
import de.afs.platform.dao.db.slick.tables.Modules
import de.afs.platform.domain._
import de.afs.platform.test.utils.InMemoryTestDatabase
import org.h2.jdbc.JdbcSQLException
import org.specs2.execute.{AsResult, Result}
import org.specs2.mutable._



class ModuleDAOSpec extends Specification with InMemoryTestDatabase {

  def hasModulesTable(implicit s: Session) = {
    val tbls = s.metaData.getTables(null, null, "%", null)
    var tableNames = List[String]()
    while (tbls.next) {
      tableNames = tableNames ::: List(tbls.getString("TABLE_NAME"))
    }

    tableNames.exists(_ == Modules.table.baseTableRow.tableName)
  }


  "The Module DAO" should {
    "have a method createTable that" should {

      "create the table" in anEmptyDatabase() {
        (db, dao, session) => implicit val s: Session = session

          dao.createTable()

          hasModulesTable must beTrue

      }
    }

    "have a method dropTable that" should {

      "drop the table" in anEmptyDatabase() {
        (db, dao, session) => implicit val s: Session = session

          dao.createTable()
          hasModulesTable must beTrue

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

  }

  def anEmptyDatabase[T]()(test: (Database, ModuleDAO[Session], Session) => T)(implicit evidence$1: AsResult[T]): Result = {
    var result: T = null.asInstanceOf[T]


    val db = Database.forURL(URL, driver = Driver)


    val moduleDaoC = new SlickModuleDAOComponent with SlickDatabaseHandle {
      database = db
    }

    db.withSession {
      implicit s: Session =>

        result = test(db, moduleDaoC.moduleDao, s)
    }
    AsResult[T](result)
  }

  def aDatabaseWithData[T]()(test: (Module, Module, ModuleDAO[Session], Session) => T)(implicit evidence$1: AsResult[T]): Result = {
    var result: T = null.asInstanceOf[T]

    val table = Modules.table

    val db = Database.forURL(URL, driver = Driver)

    val moduleDaoC = new SlickModuleDAOComponent with SlickDatabaseHandle {
      database = db
    }

    db.withSession {
      implicit s: Session =>

        table.ddl.create



        val unsaved = new Module(0, "module")
        val saved = unsaved.copy(id = 1)

        table.insert(unsaved)


        result = test(unsaved, saved, moduleDaoC.moduleDao, s)
        table.ddl.drop
    }
    AsResult[T](result)
  }

}
