

resolvers += "softprops-maven" at "http://dl.bintray.com/content/softprops/maven"

resolvers ++= Seq("snapshots", "releases").map(Resolver.sonatypeRepo)


addSbtPlugin("io.spray" % "sbt-revolver" % "0.7.2")

addSbtPlugin("com.github.mpeltonen" % "sbt-idea" % "1.7.0-SNAPSHOT")
