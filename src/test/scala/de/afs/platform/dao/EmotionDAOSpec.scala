package de.afs.platform.dao


import de.afs.platform.dao.db.slick._
import de.afs.platform.dao.db.slick.tables.{Emotions, Modules, Items, Assets}
import de.afs.platform.domain._
import de.afs.platform.test.utils.InMemoryTestDatabase
import org.h2.jdbc.JdbcSQLException
import org.specs2.execute.{Result, AsResult}
import org.specs2.mutable._
import de.afs.platform.dao.db.slick.Driver.simple._


class EmotionDAOSpec extends Specification with InMemoryTestDatabase {

  def hasEmotionsTable(implicit s: Session) = {
  val tbls = s.metaData.getTables(null, null, "%", null)
  var tableNames = List[String]()
  while (tbls.next) {
    tableNames = tableNames ::: List(tbls.getString("TABLE_NAME"))
  }

  tableNames.exists(_ == Emotions.table.baseTableRow.tableName)
}

  val module = new Module(1, "module")
  val item = new Item(1, 0, module.id, None, 1)

  "The Emotion DAO" should {
    "have a method createTable that" should {

      "create the table" in anEmptyDatabase() {
        (db, dao, session) => implicit val s: Session = session

          dao.createTable()

          hasEmotionsTable must beTrue

      }
    }

    "have a method dropTable that" should {

      "drop the table" in anEmptyDatabase() {
        (db, dao, session) => implicit val s: Session = session

          dao.createTable()
          hasEmotionsTable must beTrue

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

    "have a method emotionForItem that" should {

      "retrieve the associated Emotion" in aDatabaseWithData() {
        (unsaved, saved, dao, session) => implicit val s: Session = session
          dao.emotionForItem(item) must_== saved
      }

    }

    "have a method itemsForEmotion that" should {

      "retrieve the associated Items" in aDatabaseWithData() {
        (unsaved, saved, dao, session) => implicit val s: Session = session

          dao.itemsForEmotion(saved).items must haveLength(1)
          dao.itemsForEmotion(saved).emotion must_== saved
          dao.itemsForEmotion(saved).items must contain(item)
      }


      "return an empty list if no Items are associated with the module" in aDatabaseWithData() {
        (unsaved, saved, dao, session) => implicit val s: Session = session
          val emo = Emotion(2, "unsaved emotion")
          dao.itemsForEmotion(emo).items must beEmpty
          dao.itemsForEmotion(emo).emotion must_== emo

      }
    }
  }

  def anEmptyDatabase[T]()(test: (Database, EmotionDAO[Session], Session) => T)(implicit evidence$1: AsResult[T]): Result = {
    var result: T = null.asInstanceOf[T]


    val db = Database.forURL(URL, driver = Driver)


    val emotionDaoC = new SlickEmotionDAOComponent with SlickDatabaseHandle {
      database = db
    }

    db.withSession {
      implicit s: Session =>

        result = test(db, emotionDaoC.emotionDao, s)
    }
    AsResult[T](result)
  }

  def aDatabaseWithData[T]()(test: (Emotion, Emotion, EmotionDAO[Session], Session) => T)(implicit evidence$1: AsResult[T]): Result = {
    var result: T = null.asInstanceOf[T]

    val table = Emotions.table

    val db = Database.forURL(URL, driver = Driver)

    val emotionDaoC = new SlickEmotionDAOComponent with SlickDatabaseHandle {
      database = db
    }

    db.withSession {
      implicit s: Session =>

        (table.ddl  ++ Modules.table.ddl ++ Items.table.ddl).create


        Modules.table.insert(module)

        val unsaved = new Emotion(0, "emotion")
        val saved = unsaved.copy(id = 1)

        table.insert(unsaved)

        Items.table.insert(item)

        result = test(unsaved, saved, emotionDaoC.emotionDao, s)
        (table.ddl  ++ Modules.table.ddl ++ Items.table.ddl).drop
    }
    AsResult[T](result)
  }

}
