package de.cm.utrackr.service.web.json

import java.net.URI

import com.typesafe.scalalogging.LazyLogging
import de.afs.platform.common.utils.EncodedId
import de.afs.platform.domain.User
import de.afs.platform.service.web.dto._
import de.cm.utrackr.domain.User
import de.cm.utrackr.service.web.dto.{CheckGameResultRequest, NewGameSessionRequest, LoginDataRequest}
import spray.json._


object DomainObjectProtocol extends DefaultJsonProtocol {

  implicit object EncodedIdFormat extends RootJsonFormat[EncodedId] with LazyLogging {
    def write(obj: EncodedId) = {
      JsString(obj.encoded)
    }

    def read(json: JsValue) = json match {
      case JsString(encodedId) => EncodedId.decode(encodedId).getOrElse(deserializationError("Expected EncodedId, got: " + encodedId))
      case other => deserializationError("Expected EncodedId, got: " + other)
    }
  }

  implicit object URIFormat extends RootJsonFormat[URI] {
    def write(obj: URI) = JsString(obj.toString)

    def read(json: JsValue) = json match {
      case JsString(uri) => new URI(uri)
      case other => deserializationError("Expected URI, got: " + other)
    }
  }


  implicit val userFormat = jsonFormat5(User)

  //
  // Requests
  //

  implicit val loginDataRequestFormat = jsonFormat2(LoginDataRequest)
  implicit val newGameSessionRequestFormat = jsonFormat3(NewGameSessionRequest)
  implicit val checkGameResultRequestFormat = jsonFormat2(CheckGameResultRequest)

  //
  // Responses
  //

  implicit val videoAssetResponseFormat = jsonFormat4(AssetResponse)
  implicit val checkGameResultResponseFormat = jsonFormat3(CheckGameResultResponse)
  implicit val nextTrialsResponseFormat = jsonFormat3(NextTrialsResponse)
  implicit val newGameSessionResponseFormat = jsonFormat1(NewGameSessionResponse)
  implicit val loginDataResponseFormat = jsonFormat2(UserDataResponse)
  implicit val resetPasswordDataRequestFormat = jsonFormat1(ResetPasswordDataRequest)
  implicit val tokenDataResponseFormat = jsonFormat1(TokenDataResponse)
}
