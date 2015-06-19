package de.cm.utrackr.common.config

import org.joda.time.Duration

/**
 * Required configuration properties for authentication.
 */
trait AuthenticationConfiguration {

  /**
   * The secret for signing tokens.
   * @return The secret.
   */
  def secret: String

  /**
   * Duration until a token expires.
   * @return A duration.
   */
  def tokenExpiration: Duration

  /**
   * The name of the algorithm used to sign tokens.
   * @return The string name.
   */
  def algorithmName: String

}
