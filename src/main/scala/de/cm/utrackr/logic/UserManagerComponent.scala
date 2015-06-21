package de.cm.utrackr.logic

import de.cm.utrackr.domain.User

/**
 *
 * Business logic for managing User domain objects.
 */
trait UserManager {

  /**
   * Saves a user to the database, updates if it's id already exists.
   * @param user The user to be saved.
   * @return The saved user object (shallow copy with the correct id set)
   */
  def save(user: User): User

  /**
   * Deletes a user from the database.
   * @param user The user to be deleted.
   * @return The user object (shallow copy with the id set to 0)
   */
  def delete(user: User): User

  /**
   * Resets the password of the provided user and generates a UUID for identification
   * @param user The user.
   * @return The updated user.
   */
  def resetPasswordOf(user: User): User


  /**
   * Tries to find a user with the given id.
   * @param id The id to be queried.
   * @return The corresponding user object.
   */
  def findUserById(id: Integer): Option[User]

  /**
   * Retrieves a user.
   * @param email The email on record.
   * @param password The password on record.
   * @return The corresponding user object.
   */
  def findUserByMailAndPassword(email: String, password: String): Option[User]

  /**
   * Retrieves a user by an identification token.
   * @param token A UUID token.
   * @return The corresponding user object.
   */
  def findUserByToken(token: String): Option[User]

  /**
   * Resets a user's password.
   * @param email The user's email address.
   * @return Success.
   */
  def findUserByMail(email: String): Option[User]

}


trait UserManagerComponent {

  def manager: UserManagerInterface

  trait UserManagerInterface extends UserManager

}
