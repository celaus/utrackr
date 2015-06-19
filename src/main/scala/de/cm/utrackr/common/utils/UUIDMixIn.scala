package de.cm.utrackr.common.utils

import java.util.UUID

/**
 * A MixIn to generate and validate UUIDs. Wraps java.util.UUID.
 */
trait UUIDMixIn {
  private val uuidExpression = "^[0-9a-fA-F]{8}-?[0-9a-fA-F]{4}-?4[0-9a-fA-F]{3}-?[89abAB][0-9a-fA-F]{3}-?[0-9a-fA-F]{12}$".r

  /**
   * Creates a new random UUID
   * @return A random UUID
   */
  def newId: String = UUID.randomUUID.toString

  /**
   * Checks if the provided UUID is valid with a RegEx
   * @param uuid The UUID to validate.
   * @return True if valid.
   */
  def isAValidId(uuid: String): Boolean = uuidExpression.findFirstIn(uuid.toLowerCase).nonEmpty
}
