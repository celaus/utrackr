package de.cm.utrackr.service.web.utils

import de.cm.utrackr.common.config.AppConfig
import de.cm.utrackr.common.utils.EncodedId
import spray.routing.{PathMatcher, PathMatcher1}

object PathMatchers extends AppConfig.HashId {
  lazy val HashEncodedId: PathMatcher1[EncodedId] =
    PathMatcher( s"""[\\da-zA-Z]{$minHashLength,}""".r) flatMap EncodedId.decode
}