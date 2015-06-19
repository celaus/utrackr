package de.cm.utrackr.common.config.modules

/**
 * Provides configurable parameters for the module FacePuzzle
 */
trait FacePuzzleConfiguration extends ModuleSessionConfiguration {

  def maxDistractorCount: Int = 1
}
