package de.cm.utrackr.service.web.modules

import de.afs.platform.domain.Module
import de.afs.platform.logic.TrialManager
import de.afs.platform.service.web.ModuleServiceApi

/**
 * The WhoSpeaks-specific API implementation.
 */
trait WhoSpeaksModuleServiceApi extends ModuleServiceApi {

  val whoSpeaksTrialManager: TrialManager
  val whoSpeaksModule: Module
  val modulesPrefix: String

  private val authRealm = "whospeaks"

  lazy val whoSpeaksServiceRoute = makeRoutes(modulesPrefix, "whospeaks", DefaultModuleServiceApiImplementation(whoSpeaksModule, whoSpeaksTrialManager, authRealm))
}
