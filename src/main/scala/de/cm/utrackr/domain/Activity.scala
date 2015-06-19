package de.cm.utrackr.domain

import org.joda.time.DateTime



case class Activity (user: User, name: String, description: String, data: Seq[ActivityData])