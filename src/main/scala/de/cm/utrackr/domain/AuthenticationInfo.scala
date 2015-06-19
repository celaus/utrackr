package de.cm.utrackr.domain

import com.github.nscala_time.time.Imports._


/**
 * Represents authentication info.
 * @param user The associated user.
 * @param expires Expiration date/time.
 */
case class AuthenticationInfo(user: User, expires: DateTime) {}
