package de.cm.utrackr.common.config.modules

/**
 * Provides configurable parameters for the module FacePuzzle
 */
trait FilmPuzzleConfiguration extends ModuleSessionConfiguration {

  def maxDistractorCount: Int = 1
}
