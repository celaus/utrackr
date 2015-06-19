package de.afs.platform.test.utils

import com.mchange.v2.c3p0.ComboPooledDataSource
import de.afs.platform.common.utils.UUIDMixIn

/**
 *
 */
trait InMemoryTestDatabase extends UUIDMixIn {

  val Driver = "org.h2.Driver"


  def URL = s"jdbc:h2:mem:$newId;DATABASE_TO_UPPER=false;"

  def dataSource = {
    val ds = new ComboPooledDataSource()
    ds.setDriverClass(Driver); //loads the jdbc driver
    ds.setJdbcUrl("jdbc:h2:mem:")
    ds
  }

  def closePool(pool: ComboPooledDataSource) = pool.close()
}
