package de.cm.utrackr.common.config.modules

/**
 *
 */
trait ModuleSessionConfiguration {

  /**
   * Maximum number of trials in a session
   * @return A number.
   */
  def maxSessionLength = 10

  /**
   * Maximum number of simultaneous trials
   * @return A number.
   */
  def maxTrials = 4

  /**
   * Maximum value for difficulty.
   * @return A number.
   */
  def maxDifficulty = 10


}
