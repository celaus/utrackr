package de.afs.platform.utils

import de.afs.platform.common.utils.UUIDMixIn
import org.specs2.mutable._


class UUIDMixInSpec extends Specification {
  val wellFormedUUID = "[0-9a-fA-F]{8}-?[0-9a-fA-F]{4}-?4[0-9a-fA-F]{3}-?[89abAB][0-9a-fA-F]{3}-?[0-9a-fA-F]{12}".r
  val uuidGenerator = new UUIDMixIn {}


  val aValidUUID = "767ca43f-9f84-4794-a2a6-830bc872cc06"

  // can be parsed with java.utils.UUID.fromString
  val shiftyValidUUID = "00000000000" + aValidUUID
  val anInvalidUUID = "no-UUID"

  "UUIDTool" should {
    "create UUIDs according to the specification" in {
      wellFormedUUID.findFirstMatchIn(uuidGenerator.newId) must beSome
    }

    "correctly validate UUIDs" ! uuidGenerator.isAValidId(aValidUUID)

    "classify UUIDs with leading 0s as invalid" in {
      uuidGenerator.isAValidId(shiftyValidUUID) must beFalse
    }

    "throw an Exception if an invalid UUID is passed in" in {
      uuidGenerator.isAValidId(anInvalidUUID) must beFalse
    }
  }

}
