package de.cm.utrackr.common.config

import com.typesafe.config.ConfigFactory
import org.joda.time.Duration

object AppConfig {
  private lazy val config = ConfigFactory.load().getConfig("webapp")

  trait WebApp extends WebAppConfiguration {

    override def createTables = config.getBoolean("ddl")

    override def interface = config.getString("interface")

    override def port = config.getInt("port")
  }

  trait DatabaseDriver extends DatabaseDriverConfig {
    private lazy val section = config.getConfig("database.driver")

    def driverName = section.getString("name")
  }

  trait Authentication extends AuthenticationConfiguration {
    private lazy val section = config.getConfig("authentication")

    def secret: String = section.getString("secret")

    def tokenExpiration: Duration = Duration.parse(section.getString("token.expiration"))

    def algorithmName: String = section.getString("algorithm.name")
  }

  trait HashId extends HashIdConfiguration {

    private lazy val section = config.getConfig("hashids")

    override def salt = section.getString("salt")

    override def minHashLength = section.getInt("min.length")
  }



}
