package de.cm.utrackr.common.utils

import de.cm.utrackr.common.config.AppConfig
import org.hashids._

/**
 * A class to obfuscate numeric ids as hashes.
 * @param id The numeric id to obfuscate
 */
case class EncodedId(id: Long) extends AppConfig.HashId {
  def encoded = Hashids(salt = salt, minHashLength = minHashLength).encode(id)
}

object EncodedId extends AppConfig.HashId {

  def decode(encodedId: String): Option[EncodedId] = try {
    Hashids(salt = salt, minHashLength = minHashLength).decode(encodedId).headOption match {
      case Some(long) => Some(EncodedId(long))
      case _ => None
    }
  }
  catch {
    case e: Exception => None
  }
}
