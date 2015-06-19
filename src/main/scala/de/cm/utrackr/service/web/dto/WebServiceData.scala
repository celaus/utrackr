package de.cm.utrackr.service.web.dto

import java.net.URI

import de.afs.platform.common.utils.EncodedId
import de.afs.platform.domain.{TrialData, Asset}

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

/**
 * Data passed in when requesting a new game session.
 * @param session Session length in sets of trials
 * @param trials Number of trials per set
 * @param difficulty Difficulty indicator of the session
 */
case class NewGameSessionRequest(session: Int, trials: Int, difficulty: Int)

/**
 * Data passed in to check whether the matching is correct.
 * @param trialId The trial that is being solved.
 * @param assetIds The asset ids to check.
 */
case class CheckGameResultRequest(trialId: EncodedId, assetIds: Seq[EncodedId])


//
// RESPONSES
//

/**
 * Data passed in to check whether the matching is correct.
 * @param completedAsset The asset when completed.
 * @param assetIds The asset ids to check.
 * @param matching Whether the assets match.
 */
case class CheckGameResultResponse(completedAsset: Option[AssetResponse], assetIds: Seq[EncodedId], matching: Boolean)

/**
 * Response to when a new game session is requested.
 * @param gameId The id for the game session.
 */
case class NewGameSessionResponse(gameId: EncodedId)

/**
 * Response helper for returning video-based assets.
 * @param id The encoded id of the asset.
 * @param uri Where to find the corresponding video
 * @param mime The MIME type of the asset.
 * @param trialId The encoded id of the corresponding trial.
 */
case class AssetResponse(id: EncodedId, uri: URI, mime: String, trialId: Option[EncodedId] = None)

/**
 * Companion object for case class. Facilitates conversion.
 */
object AssetResponseUtil {
  def fromAsset(a: Asset, trial: Option[TrialData] = None) = if (trial.isDefined)
    new AssetResponse(EncodedId(a.id.toLong), URI.create(a.location), a.mime, Some(EncodedId(trial.get.id.toLong)))
  else
    new AssetResponse(EncodedId(a.id.toLong), URI.create(a.location), a.mime)
}

/**
 * Response to when a new set of trials is requested.
 * @param sessionId The id for the game session.
 * @param assets The assets to be delivered.
 * @param explicits The challenges for the explicit parts.
 */
case class NextTrialsResponse(sessionId: EncodedId, assets: Map[String, Seq[AssetResponse]], explicits: Seq[(EncodedId, String)])

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
