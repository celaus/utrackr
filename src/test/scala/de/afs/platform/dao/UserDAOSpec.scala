package de.afs.platform.dao

import de.afs.platform.dao.db.slick.tables.Users
import de.afs.platform.domain.User
import de.afs.platform.dao.db.slick.{SlickDatabaseHandle, SlickUserDAOComponent}
import de.afs.platform.test.utils.InMemoryTestDatabase
import org.h2.jdbc.JdbcSQLException
import org.specs2.execute.{AsResult, Result}
import org.specs2.mutable._

import de.afs.platform.dao.db.slick.Driver.simple._
                                         //import scala.slick.driver.H2Driver.simple._

/**
 *
 */
class UserDAOSpec extends Specification with InMemoryTestDatabase {

  def hasUsersTable(implicit s: Session) = {
    val tbls = s.metaData.getTables(null, null, "%", null)
    var tableNames = List[String]()
    while (tbls.next) {
      tableNames = tableNames ::: List(tbls.getString("TABLE_NAME"))
    }

    tableNames.exists(_ == Users.table.baseTableRow.tableName)
  }

  "The User DAO" should {
    "have a method createTable that" should {

      "create the table" in anEmptyDatabase() {
        (db, userDao, session) => implicit val s: Session = session

          userDao.createTable()

          hasUsersTable must beTrue

      }
    }

    "have a method dropTable that" should {

      "drop the table" in anEmptyDatabase() {
        (db, userDao, session) => implicit val s: Session = session

          userDao.createTable()
          hasUsersTable must beTrue

          userDao.dropTable() must not(throwA[JdbcSQLException])

      }

    }

    "have a method getById that" should {

      "return the correct user object for an id" in aDatabaseWithData() {
        (unsavedUser, savedUser, userDao, session) => implicit val s: Session = session
          userDao.getById(savedUser.id) must_== (Some(savedUser))
      }

      "return None if an id was not found" in aDatabaseWithData() {
        (unsavedUser, savedUser, userDao, session) => implicit val s: Session = session

          userDao.getById(123) must beNone
      }
    }

    "have a method getByToken that" should {

      "return the correct user object for a token" in aDatabaseWithData() {
        (unsavedUser, savedUser, userDao, session) => implicit val s: Session = session

          val token = "a token"
          val userWithToken = userDao.save(new User(0, "user@user.com", "secret", false, Option(token)))
          userDao.getByToken(token) must_== (Some(userWithToken))
      }

      "return None if a token was not found" in aDatabaseWithData() {
        (unsavedUser, savedUser, userDao, session) => implicit val s: Session = session
          userDao.getByToken("not in DB") must beNone
      }
    }

    "have a method save that" should {

      "correctly persist updates to a user object" in aDatabaseWithData() {
        (unsavedUser, savedUser, userDao, session) => implicit val s: Session = session

          val token = "a token"
          savedUser.token = Some(token)

          (userDao.save(savedUser)) must_== savedUser

          userDao.getById(savedUser.id) must beSome(savedUser)
      }

      "create a new database entry if a user was not persisted" in aDatabaseWithData() {
        (unsavedUser, savedUser, userDao, session) => implicit val s: Session = session

          val newUser = userDao.save(unsavedUser)
          (newUser.id != savedUser.id) must beTrue

          userDao.getById(newUser.id) must beSome(newUser)
      }

      "increment the id of a newly persisted object" in aDatabaseWithData() {
        (unsavedUser, savedUser, userDao, session) => implicit val s: Session = session

          val newUser = userDao.save(unsavedUser)
          newUser.id must be_>  (savedUser.id)
      }
    }

    "have a method delete that" should {

      "correctly delete a user object" in aDatabaseWithData() {
        (unsavedUser, savedUser, userDao, session) => implicit val s: Session = session

          userDao.delete(savedUser) must_== unsavedUser

          userDao.getById(savedUser.id) must beNone
      }

      "set the id to 0 on deletion" in aDatabaseWithData() {
        (unsavedUser, savedUser, userDao, session) => implicit val s: Session = session


          userDao.delete(savedUser).id must_== 0
      }

      "do nothing if an object is not in the database" in aDatabaseWithData() {
        (unsavedUser, savedUser, userDao, session) => implicit val s: Session = session

          userDao.delete(unsavedUser) must_== unsavedUser
      }
    }

    "have a method getBy that" should {

      "return None if an email/password combination was not found" in aDatabaseWithData() {
        (unsavedUser, savedUser, userDao, session) => implicit val s: Session = session

          userDao.getBy("e@mail.com", "nopassword") must beNone
      }

      "return the correct user object for an email/password combination" in aDatabaseWithData() {
        (unsavedUser, savedUser, userDao, session) => implicit val s: Session = session

          userDao.getBy(savedUser.email, savedUser.password) must beSome(savedUser)
      }
    }

    "have a method getByMail that" should {

      "return None if an email was not found" in aDatabaseWithData() {
        (unsavedUser, savedUser, userDao, session) => implicit val s: Session = session

          userDao.getByMail("e@mail.com") must beNone
      }

      "return the correct user object for an email" in aDatabaseWithData() {
        (unsavedUser, savedUser, userDao, session) => implicit val s: Session = session

          userDao.getByMail(savedUser.email) must beSome(savedUser)
      }
    }

  }

  def anEmptyDatabase[T]()(test: (Database, UserDAO[Session], Session) => T)(implicit evidence$1: AsResult[T]): Result = {
    var result: T = null.asInstanceOf[T]

    val userDaoComponent =
      new SlickUserDAOComponent
        with SlickDatabaseHandle {
        database = Database.forURL(URL, driver = Driver)
      }
    val userDao = userDaoComponent.userDao


    userDaoComponent.database.withSession {
      implicit s: Session =>
        result = test(userDaoComponent.database, userDao, s)
    }
    AsResult[T](result)
  }

  def aDatabaseWithData[T]()(test: (User, User, UserDAO[Session], Session) => T)(implicit evidence$1: AsResult[T]): Result = {
    var result: T = null.asInstanceOf[T]
    val unsavedUser = new User(0, "user@user.com", "secret", false, None)
    val savedUser = unsavedUser.copy(id = 1)
    val table = Users.table

    val userDaoComponent =
      new SlickUserDAOComponent
        with SlickDatabaseHandle {
        database = Database.forURL(URL, driver = Driver)
      }
    val userDao = userDaoComponent.userDao


    userDaoComponent.database.withSession {
      implicit s: Session =>

        table.ddl.create
        table.insert(unsavedUser)

        result = test(unsavedUser, savedUser, userDao, s)
        table.ddl.drop
    }
    AsResult[T](result)
  }
}
