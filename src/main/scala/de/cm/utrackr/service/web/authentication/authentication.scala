package de.cm.utrackr.service.web

import scala.concurrent.Future

/**
 *
 */
package object authentication {
  type TokenAuthenticator[T] = Option[ Map[String, String]] ⇒ Future[Option[T]]
}
