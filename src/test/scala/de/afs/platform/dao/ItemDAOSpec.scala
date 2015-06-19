package de.afs.platform.dao

import de.afs.platform.dao.db.slick._
import de.afs.platform.dao.db.slick.tables.{Emotions, Modules, Items, Assets}
import de.afs.platform.domain._
import de.afs.platform.test.utils.InMemoryTestDatabase
import org.h2.jdbc.JdbcSQLException
import org.specs2.execute.{Result, AsResult}
import org.specs2.mutable._
import de.afs.platform.dao.db.slick.Driver.simple._

/**
 *
 */
class ItemDAOSpec extends Specification with InMemoryTestDatabase {


  def hasItemsTable(implicit s: Session) = {
    val tbls = s.metaData.getTables(null, null, "%", null)
    var tableNames = List[String]()
    while (tbls.next) {
      tableNames = tableNames ::: List(tbls.getString("TABLE_NAME"))
    }

    tableNames.exists(_ == Items.table.baseTableRow.tableName)
  }


  val module = new Module(1, "module")
  val emotion = new Emotion(1, "emotion")

  val parts = List(
    Asset(1, "mime", "/loc/to/asset", 0, Some(1), None),
    Asset(2, "mime", "/loc/to/asset", 0, Some(1), None),
    Asset(3, "mime", "/loc/to/asset", 2, Some(1), None)
  )
  val completedAsset = Asset(4, "mime", "/loc/to/asset", 0, Some(1), None)

  "The Item DAO" should {
    "have a method createTable that" should {

      "create the table" in anEmptyDatabase() {
        (db, dao, session) => implicit val s: Session = session

          dao.createTable()

          hasItemsTable must beTrue

      }
    }

    "have a method dropTable that" should {

      "drop the table" in anEmptyDatabase() {
        (db, dao, session) => implicit val s: Session = session

          dao.createTable()
          hasItemsTable must beTrue

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

    "have a method partsOfItem that" should {

      "retrieve the associated Assets" in aDatabaseWithData() {
        (unsaved, saved, dao, session) => implicit val s: Session = session
          dao.partsOfItem(saved, List(0, 2)).assets must containAllOf(parts)
          dao.partsOfItem(saved).item must_== saved

      }

      "retrieve the associated Assets according to their classification" in aDatabaseWithData() {
        (unsaved, saved, dao, session) => implicit val s: Session = session
          dao.partsOfItem(saved, List(0)).assets must containAllOf(parts.take(2))
          dao.partsOfItem(saved, List(0)).item must_== saved

          dao.partsOfItem(saved, List(1)).assets must beEmpty

          dao.partsOfItem(saved, List(0, 1)).assets must containAllOf(parts.take(2))
          dao.partsOfItem(saved, List(0, 1)).item must_== saved


          dao.partsOfItem(saved, List(0, 2)).assets must containAllOf(parts)
          dao.partsOfItem(saved, List(0, 2)).item must_== saved

          dao.partsOfItem(saved, List(2)).assets must containAllOf(List(parts(2)))
          dao.partsOfItem(saved, List(2)).item must_== saved
      }

      "return an empty list of parts if no parts are available" in aDatabaseWithData() {
        (unsaved, saved, dao, session) => implicit val s: Session = session
          dao.partsOfItem(unsaved).assets must beEmpty
      }
    }

    "have a method itemsFor that" should {

      "retrieve the associated Items" in aDatabaseWithData() {
        (unsaved, saved, dao, session) => implicit val s: Session = session

          dao.itemsFor(module, 1) must haveLength(1)
          dao.itemsFor(module, 1) must contain(saved)
      }

      "retrieve the requested number or all available Items" in aDatabaseWithData() {
        (unsaved, saved, dao, session) => implicit val s: Session = session
          1 to 5 foreach (i => dao.save(unsaved))

          dao.itemsFor(module, 1) must haveLength(1)
          dao.itemsFor(module, 3) must haveLength(3)
          dao.itemsFor(module, 10) must haveLength(6)
          dao.itemsFor(module, 0) must beEmpty
          dao.itemsFor(module, -1) must beEmpty
      }

      "return an empty list if no Items are associated with the module" in aDatabaseWithData() {
        (unsaved, saved, dao, session) => implicit val s: Session = session
          dao.itemsFor(Module(2, "unsaved module"), 2) must beEmpty
      }
    }

    "have a method completedAssetForItem that" should {

      "retrieve the associated Asset" in aDatabaseWithData() {
        (unsaved, saved, dao, session) => implicit val s: Session = session

          dao.completedAssetForItem(saved) must beSome(ItemWithCompletedAsset(saved, completedAsset))
      }

      "return an None if no associated Asset can be found" in aDatabaseWithData() {
        (unsaved, saved, dao, session) => implicit val s: Session = session
          dao.completedAssetForItem(unsaved.copy(completedAssetId = None)) must beNone
      }
    }

    "have a method fromParts that" should {

      "retrieve the associated Item" in aDatabaseWithData() {
        (unsaved, saved, dao, session) => implicit val s: Session = session

          dao.fromParts(parts, List(0, 2)) must beSome(saved)
      }

      "return an None if no Item can be matched to the parts" in aDatabaseWithData() {
        (unsaved, saved, dao, session) => implicit val s: Session = session
          dao.fromParts(parts take 2) must beNone
      }

      "return an None if the classification does not include all required parts" in aDatabaseWithData() {
        (unsaved, saved, dao, session) => implicit val s: Session = session
          dao.fromParts(parts, List(0)) must beNone
      }
    }
  }

  def anEmptyDatabase[T]()(test: (Database, ItemDAO[Session], Session) => T)(implicit evidence$1: AsResult[T]): Result = {
    var result: T = null.asInstanceOf[T]


    val db = Database.forURL(URL, driver = Driver)


    val itemDaoC = new SlickItemDAOComponent with SlickDatabaseHandle {
      database = db
    }

    db.withSession {
      implicit s: Session =>

        (Modules.table.ddl ++ Emotions.table.ddl).create
        result = test(db, itemDaoC.itemDao, s)
    }
    AsResult[T](result)
  }

  def aDatabaseWithData[T]()(test: (Item, Item, ItemDAO[Session], Session) => T)(implicit evidence$1: AsResult[T]): Result = {
    var result: T = null.asInstanceOf[T]



    val table = Items.table

    val db = Database.forURL(URL, driver = Driver)


    val itemDaoC = new SlickItemDAOComponent with SlickDatabaseHandle {
      database = db
    }

    db.withSession {
      implicit s: Session =>

        (table.ddl ++ Modules.table.ddl ++ Emotions.table.ddl ++ Assets.table.ddl).create


        Modules.table.insert(module)
        Emotions.table.insert(emotion)

        val unsaved = new Item(0, parts.length, module.id, Some(completedAsset.id), emotion.id)
        val saved = unsaved.copy(id = 1)

        table.insert(unsaved)

        parts.foreach(a => Assets.table.insert(a))
        Assets.table.insert(completedAsset)

        result = test(unsaved, saved, itemDaoC.itemDao, s)
        (table.ddl ++ Modules.table.ddl ++ Emotions.table.ddl ++ Assets.table.ddl).drop
    }
    AsResult[T](result)
  }

}
