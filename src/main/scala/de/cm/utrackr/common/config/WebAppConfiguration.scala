package de.cm.utrackr.common.config

/**
 * Configuration for the web app/web service.
 */
trait WebAppConfiguration {

  /**
   * Setting whether to execute DDL on start.
   * @return True/False.
   */
  def createTables:Boolean

  /**
   * Which interface to bind the web service to. (IP Addresses, ::0 for all IPv4/v6 interfaces)
   * @return IP address to bind to.
   */
  def interface: String

  /**
   * Which port to use.
   * @return A port number.
   */
  def port: Int
}
