package de.cm.utrackr.service.web.utils

import de.afs.platform.common.config.{AppConfig, HashIdConfiguration}
import de.afs.platform.common.utils.EncodedId
import spray.routing.{PathMatcher, PathMatcher1}

object PathMatchers extends AppConfig.HashId {
  lazy val HashEncodedId: PathMatcher1[EncodedId] =
    PathMatcher( s"""[\\da-zA-Z]{$minHashLength,}""".r) flatMap EncodedId.decode
}