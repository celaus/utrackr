package de.cm.utrackr.common.config.modules

/**
 * Provides configurable parameters for the module FacePuzzle
 */
trait WhoSpeaksConfiguration extends ModuleSessionConfiguration {

  def maxAudioDistractorCount: Int = 1

  def maxDistractorCount: Int = 1

  def congruentClassification: Int = 0

  def notCongruentClassification: Int = 1
}
