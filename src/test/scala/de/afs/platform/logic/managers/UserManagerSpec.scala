package de.afs.platform.logic.managers

import de.afs.platform.domain.User
import de.afs.platform.dao.db.slick.{SlickDatabaseHandle, SlickUserDAOComponent}
import de.afs.platform.logic.{UserManager, UserNotFoundException}
import de.afs.platform.test.utils.InMemoryTestDatabase
import org.specs2.execute.{AsResult, Result}
import org.specs2.mutable._

import scala.slick.driver.H2Driver.simple._


class UserManagerSpec extends Specification with InMemoryTestDatabase {


  "A UserManager" should {

    "have a method save that" should {

      "save and returns an object with the id set" in aDatabaseWithoutData() { (unsavedUser, savedUser, userManager) =>
        userManager.save(unsavedUser) must_== (savedUser)
      }

      "save and correctly increments the assigned id" in aDatabaseWithoutData() { (unsavedUser, savedUser, userManager) =>
        val anotherSavedUser = savedUser.copy(id = 2)
        userManager.save(unsavedUser) must_== (savedUser)
        userManager.save(unsavedUser) must_== (anotherSavedUser)
      }

      "update users if persistent" in aDatabaseWithData() { (unsavedUser, savedUser, userManager) =>
        val newEmail = "a@b.com"
        savedUser.email = newEmail
        userManager.save(savedUser) must_== (savedUser)
        val savedUserFromDB = userManager.findUserById(savedUser.id)
        savedUserFromDB must beSome(savedUser)

        savedUserFromDB.get.email must_== (newEmail)
      }
    }


    "have a method delete that" should {
      "delete a previously saved user" in aDatabaseWithData() { (unsavedUser, savedUser, userManager) =>
        userManager.findUserById(1) must beSome(savedUser)
        userManager.delete(savedUser) must_== (unsavedUser)
        userManager.findUserById(1) must beNone
      }

    }

    "have a method resetPasswordOf that" should {
      "provide a token for a user to change the password" in aDatabaseWithData() { (unsavedUser, savedUser, userManager) =>
        savedUser.token must beNone and (
          (userManager.resetPasswordOf(savedUser) must_== (savedUser))) and (
          savedUser.token must not beNone
          )
      }
    }


    "have a method findUserById that" should {

      "return the corresponding user object for an id" in aDatabaseWithData() { (unsavedUser, savedUser, userManager) =>
        userManager.findUserById(1) must beSome(savedUser)
      }

      "return None for invalid ids" in aDatabaseWithData() { (unsavedUser, savedUser, userManager) =>
        userManager.findUserById(0) must beNone
        userManager.findUserById(1123) must beNone
        userManager.findUserById(-1) must beNone
        userManager.findUserById(Int.MaxValue) must beNone
        userManager.findUserById(Int.MinValue) must beNone
      }
    }

    "have a method findUserByMailAndPassword that" should {

      "return None for invalid credentials" in aDatabaseWithData() { (unsavedUser, savedUser, userManager) =>
        userManager.findUserByMailAndPassword("someone@abc.com", "secret") must beNone
        userManager.findUserByMailAndPassword("12345", "hello") must beNone
        userManager.findUserByMailAndPassword("user@user.com", "no-secret") must beNone
        userManager.findUserByMailAndPassword("", "secret") must beNone
        userManager.findUserByMailAndPassword("", "") must beNone
        userManager.findUserByMailAndPassword("a' OR 1=1 ", "") must beNone

      }

      "return the user object on correct credentials" in aDatabaseWithData() { (unsavedUser, savedUser, userManager) =>
        userManager.findUserByMailAndPassword(unsavedUser.email, unsavedUser.password) must beSome(savedUser)
      }
    }

    "have a method findUserByToken that" should {

      "throw an Exception if a user was not found when retrieving by token" in aDatabaseWithData() { (unsavedUser, savedUser, userManager) =>
        userManager resetPasswordOf unsavedUser must throwA[UserNotFoundException]
      }
    }

    "have a method findUserByMail that" should {
      "return the user object for a correct email address" in aDatabaseWithData() { (unsavedUser, savedUser, userManager) =>
        userManager.findUserByMail(savedUser.email) must beSome(savedUser)
      }

      "return None for invalid emails" in aDatabaseWithData() { (unsavedUser, savedUser, userManager) =>
        userManager.findUserByMail("") must beNone
        userManager.findUserByMail("123") must beNone
        userManager.findUserByMail("no-a-valid-email") must beNone
        userManager.findUserByMail("; DROP TABLE USERS;") must beNone
        userManager.findUserByMail("@@@@@") must beNone
        userManager.findUserByMail("user@user.de") must beNone
      }

      "ignore SQL injection" in aDatabaseWithData() { (unsavedUser, savedUser, userManager) =>
        userManager.findUserByMail("a' OR 1=1") must beNone
      }
    }

  }

  def aDatabaseWithData[T]()(test: (User, User, UserManager) => T)(implicit evidence$1: AsResult[T]): Result = {
    var result: T = null.asInstanceOf[T]
    val unsavedUser = new User(0, "user@user.com", "secret", false, None)
    val savedUser = unsavedUser.copy(id = 1)

    val userManagerComponent =
      new DBUserManagerComponent
        with SlickUserDAOComponent
        with SlickDatabaseHandle {
        database = Database.forURL(URL, driver = Driver)
      }

    val userManager = userManagerComponent.userManager
    val userDao = userManagerComponent.userDao

    userManagerComponent.database.withSession {
      implicit s: Session =>

        userDao.createTable()
        userDao.save(unsavedUser)

        result = test(unsavedUser, savedUser, userManager)
        userDao.dropTable()
    }
    AsResult[T](result)
  }

  def aDatabaseWithoutData[T]()(test: (User, User, UserManager) => T)(implicit evidence$1: AsResult[T]): Result = {
    var result: T = null.asInstanceOf[T]
    val unsavedUser = new User(0, "user@user.com", "secret", false, None)
    val savedUser = unsavedUser.copy(id = 1)

    val userManagerComponent =
      new DBUserManagerComponent
        with SlickUserDAOComponent
        with SlickDatabaseHandle {
        database = Database.forURL(URL, driver = Driver)
      }

    val userManager = userManagerComponent.userManager
    val userDao = userManagerComponent.userDao

    userManagerComponent.database.withSession {
      implicit s: Session =>

        userDao.createTable()
        result = test(unsavedUser, savedUser, userManager)
        userDao.dropTable()
    }
    AsResult[T](result)
  }
}
