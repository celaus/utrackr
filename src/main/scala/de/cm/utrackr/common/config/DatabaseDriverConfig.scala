package de.cm.utrackr.common.config

/**
 *
 */
trait DatabaseDriverConfig {

  /**
   * The abbreviated name of the database driver.
   * @return A string containing the driver name.
   */
  def driverName: String
}
