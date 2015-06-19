package de.cm.utrackr.domain

/**
 * A data object for users.
 * @param id Numeric, unique identifier. 0 if not in sync with the database (or not persisted)
 * @param email Email address string.
 * @param password Password string - should be encrypted/hashed.
 * @param passwordReset Flag if the password is to be reset.
 * @param token Token to retrieve the object in case the password was reset.
 */
case class User(id: Int, var email: String, var password: String, var passwordReset: Boolean, var token: Option[String])
