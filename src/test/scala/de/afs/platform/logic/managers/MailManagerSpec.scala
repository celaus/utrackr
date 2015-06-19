package de.afs.platform.logic.managers

import de.afs.platform.common.config.MailServerConfiguration
import de.afs.platform.domain.EMail
import org.jvnet.mock_javamail.Mailbox
import org.specs2.mutable._
import org.specs2.time.NoTimeConversions

import scala.concurrent.Await
import scala.concurrent.duration._


class MailManagerSpec extends Specification with NoTimeConversions {
  isolated

  val mailManager = new ServiceMailManagerComponent with MailServerConfiguration {

    override def serverAddress: String = "localhost"

    override def username: String = ""

    override def authenticate: Boolean = false

    override def useStartTLS: Boolean = false

    override def password: String = ""

    override def port: Int = 25
  } mailManager

  "A MailManger" should {
    "have a method send that" should {

      "send emails to a given address with the correct subject and body" in {
        val recipient = "a@b.com"
        val body = "my body"
        val subject = "my subject"
        val sender = "test@test.com"
        val future = mailManager.send(new EMail(sender, recipient, subject, body))

        Await.ready(future, 5.seconds)

        val inbox = Mailbox.get(recipient)
        inbox.size() === 1
        val mail = inbox.get(0)
        mail.getContent === body
        mail.getSubject === subject
      }

    }
  }
}
