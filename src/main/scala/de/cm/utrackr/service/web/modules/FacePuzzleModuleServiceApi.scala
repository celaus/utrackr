package de.cm.utrackr.service.web.modules

import de.afs.platform.domain.Module
import de.afs.platform.logic.TrialManager
import de.afs.platform.service.web.ModuleServiceApi

/**
 * The FacePuzzle-specific API implementation.
 */
trait FacePuzzleModuleServiceApi extends ModuleServiceApi {

  val facePuzzleTrialManager: TrialManager
  val facePuzzleModule: Module
  val modulesPrefix: String

  private val authRealm = "facepuzzle"

  lazy val facePuzzleServiceRoute = makeRoutes(modulesPrefix, "facepuzzle", DefaultModuleServiceApiImplementation(facePuzzleModule, facePuzzleTrialManager, authRealm))
}
