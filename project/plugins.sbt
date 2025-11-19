addSbtPlugin("org.xerial.sbt" % "sbt-sonatype"  % "3.11.3")
addSbtPlugin("com.github.sbt" % "sbt-pgp"       % "2.3.1")
addSbtPlugin("org.scoverage"  % "sbt-scoverage" % "2.0.9")
addSbtPlugin("ch.epfl.scala"  % "sbt-scalafix"  % "0.11.1")

// Documentation microsite with mdoc and Laika
addSbtPlugin("org.typelevel"  % "sbt-typelevel-site" % "0.7.4")
addSbtPlugin("com.github.sbt" % "sbt-ci-release"     % "1.9.0")
