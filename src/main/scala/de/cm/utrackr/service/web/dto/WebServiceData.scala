package de.cm.utrackr.service.web.dto

import de.cm.utrackr.common.utils.EncodedId

//
// REQUESTS
//

/**
 * Data passed in via JSON to request a login.
 * @param email Email.
 * @param password Corresponding password.
 */
case class LoginDataRequest(email: String, password: String)

/**
 * Data passed in via JSON to request a password reset.
 * @param email Email.
 */
case class ResetPasswordDataRequest(email: String)

//
// RESPONSES
//
/**
 * Data sent to the client if the login is valid.
 * @param token A token to identify the user.
 */
case class TokenDataResponse(token: String)

/**
 * Data sent as a response to a user data request.
 * @param id The user id.
 * @param email Email.
 */
case class UserDataResponse(id: EncodedId, email: String)
