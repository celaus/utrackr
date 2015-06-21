package de.cm.utrackr.logic

import de.cm.utrackr.domain.User

trait TokenManager {
  def createToken(user: User): String
}


trait TokenManagerComponent {

  def manager: TokenManagerInterface

  trait TokenManagerInterface extends TokenManager

}
