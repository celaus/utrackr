package de.cm.utrackr.domain

import org.joda.time.DateTime

case class ActivityData(date: DateTime, plan: Int, actual: Int, label: Option[String])