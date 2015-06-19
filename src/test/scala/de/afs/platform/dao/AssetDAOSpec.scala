package de.afs.platform.dao

import de.afs.platform.dao.db.slick._
import de.afs.platform.dao.db.slick.tables.Assets
import de.afs.platform.domain.{Emotion, Module, Item, Asset}
import de.afs.platform.test.utils.InMemoryTestDatabase
import org.h2.jdbc.JdbcSQLException
import org.specs2.execute.{Result, AsResult}
import org.specs2.mutable._
import de.afs.platform.dao.db.slick.Driver.simple._

/**
 *
 */
class AssetDAOSpec extends Specification with InMemoryTestDatabase {
  def hasAssetsTable(implicit s: Session) = {
    val tbls = s.metaData.getTables(null, null, "%", null)
    var tableNames = List[String]()
    while (tbls.next) {
      tableNames = tableNames ::: List(tbls.getString("TABLE_NAME"))
    }

    tableNames.exists(_ == Assets.table.baseTableRow.tableName)
  }

  "The Asset DAO" should {
    "have a method createTable that" should {

      "create the table" in anEmptyDatabase() {
        (db, dao, session) => implicit val s: Session = session

          dao.createTable()

          hasAssetsTable must beTrue

      }
    }

    "have a method dropTable that" should {

      "drop the table" in anEmptyDatabase() {
        (db, dao, session) => implicit val s: Session = session

          dao.createTable()
          hasAssetsTable must beTrue

          dao.dropTable() must not(throwA[JdbcSQLException])

      }

    }

    "have a method getById that" should {

      "return the correct asset object for an id" in aDatabaseWithData() {
        (unsaved, saved, dao, session) => implicit val s: Session = session
          dao.getById(saved.id) must beSome(saved)
      }

      "return None if an id was not found" in aDatabaseWithData() {
        (unsaved, saved, dao, session) => implicit val s: Session = session

          dao.getById(123) must beNone
      }
    }


    "have a method save that" should {

      "correctly persist updates to a asset object" in aDatabaseWithData() {
        (unsaved, saved, dao, session) => implicit val s: Session = session
          dao.save(unsaved) must_== saved.copy(id = 2)
          dao.getById(saved.id) must beSome(saved)
      }

      "create a new database entry if a asset was not previously persisted" in aDatabaseWithData() {
        (unsaved, saved, dao, session) => implicit val s: Session = session
          val newAsset = dao.save(unsaved)
          newAsset.id != saved.id must beTrue

          dao.getById(newAsset.id) must beSome(newAsset)
      }

      "increment the id of a newly persisted object" in aDatabaseWithData() {
        (unsaved, saved, dao, session) => implicit val s: Session = session

          val newAsset = dao.save(unsaved)
          newAsset.id != saved.id must beTrue

          newAsset.id must be_>(saved.id)
      }
    }

    "have a method delete that" should {

      "correctly delete a asset object" in aDatabaseWithData() {
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

    "have a method forIds that" should {

      "retrieve a set of assets by the ids provided" in aDatabaseWithData() {
        (unsaved, saved, dao, session) => implicit val s: Session = session
          val assets = 1 to 10 map { i => dao.save(unsaved) }

          dao.forIds(assets map (_.id)) must containTheSameElementsAs(assets)
      }

      "return no object for invalid ids" in aDatabaseWithData() {
        (unsaved, saved, dao, session) => implicit val s: Session = session
          dao.forIds(List(-1, -2, -3, -4)) must beEmpty
      }

      "retrieve any set of ids, not including duplicates" in aDatabaseWithData() {
        (unsaved, saved, dao, session) => implicit val s: Session = session
          val duplicates = 1 to 5 map (i => saved)

          val result = dao.forIds(duplicates map (_.id))
          result must haveLength(1)
          result must contain(saved)
      }

      "retrieve any set of ids and leave out those for invalid ids" in aDatabaseWithData() {
        (unsaved, saved, dao, session) => implicit val s: Session = session
          val assets = 1 to 10 map { i => dao.save(unsaved) }

          val n = 3
          val forIds = dao.forIds((assets map (_.id) take n) ++ List(-1, -2))
          forIds must have length (n)
          forIds must containTheSameElementsAs(assets take n)
      }
    }
  }

  def anEmptyDatabase[T]()(test: (Database, AssetDAO[Session], Session) => T)(implicit evidence$1: AsResult[T]): Result = {
    var result: T = null.asInstanceOf[T]


    val db = Database.forURL(URL, driver = Driver)
    val moduleDaoC = new SlickModuleDAOComponent with SlickDatabaseHandle {
      database = db
    }

    val emotionDaoC = new SlickEmotionDAOComponent with SlickDatabaseHandle {
      database = db
    }

    val itemDaoC = new SlickItemDAOComponent with SlickDatabaseHandle {
      database = db
    }

    val assetDaoComponent = new SlickAssetDAOComponent with SlickDatabaseHandle {
      database = db
    }

    val assetDao = assetDaoComponent.assetDao


    db.withSession {
      implicit s: Session =>

        moduleDaoC.moduleDao.createTable()
        emotionDaoC.emotionDao.createTable()
        itemDaoC.itemDao.createTable()
        result = test(assetDaoComponent.database, assetDao, s)
    }
    AsResult[T](result)
  }

  def aDatabaseWithData[T]()(test: (Asset, Asset, AssetDAO[Session], Session) => T)(implicit evidence$1: AsResult[T]): Result = {
    var result: T = null.asInstanceOf[T]

    val unsaved = new Asset(0, "mime", "/path/to/location", 0, None, Some("puzzleKey"))
    val saved = unsaved.copy(id = 1)
    val table = Assets.table

    val db = Database.forURL(URL, driver = Driver)
    val moduleDaoC = new SlickModuleDAOComponent with SlickDatabaseHandle {
      database = db
    }

    val emotionDaoC = new SlickEmotionDAOComponent with SlickDatabaseHandle {
      database = db
    }

    val itemDaoC = new SlickItemDAOComponent with SlickDatabaseHandle {
      database = db
    }

    val assetDaoComponent = new SlickAssetDAOComponent with SlickDatabaseHandle {
      database = db
    }

    val assetDao = assetDaoComponent.assetDao


    db.withSession {
      implicit s: Session =>

        moduleDaoC.moduleDao.createTable()
        emotionDaoC.emotionDao.createTable()
        itemDaoC.itemDao.createTable()

        table.ddl.create
        table.insert(unsaved)

        result = test(unsaved, saved, assetDao, s)
        table.ddl.drop
    }
    AsResult[T](result)
  }

}
