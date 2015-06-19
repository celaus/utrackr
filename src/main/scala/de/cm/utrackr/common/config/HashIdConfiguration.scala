package de.cm.utrackr.common.config

/**
 * Provides configurable parameters for encoding numbers with Hashids
 */
trait HashIdConfiguration {

  /**
   * The salt to randomize the alphabet with.
   * @return A string to randomize with.
   */
  def salt:String

  /**
   * Minimum length of an encoded string.
   * @return The length.
   */
  def minHashLength: Int
}
