package de.cm.utrackr.service.web.modules

import de.afs.platform.domain.Module
import de.afs.platform.logic.TrialManager
import de.afs.platform.service.web.ModuleServiceApi

/**
 * The FilmPuzzle-specific API implementation.
 */
trait FilmPuzzleModuleServiceApi extends ModuleServiceApi {

  val filmPuzzleTrialManager: TrialManager
  val filmPuzzleModule: Module
  val modulesPrefix: String

  private val authRealm = "filmpuzzle"

  lazy val filmPuzzleServiceRoute = makeRoutes(modulesPrefix, "filmpuzzle", DefaultModuleServiceApiImplementation(filmPuzzleModule, filmPuzzleTrialManager, authRealm))
}
