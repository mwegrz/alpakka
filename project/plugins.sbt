addSbtPlugin("de.heikoseeberger" % "sbt-header" % "5.1.0")
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.0.0-RC5")
addSbtPlugin("com.dwijnand" % "sbt-dynver" % "3.3.0")
addSbtPlugin("com.lightbend.paradox" % "sbt-paradox" % "0.5.3")
addSbtPlugin("com.lightbend.paradox" % "sbt-paradox-dependencies" % "0.1")
addSbtPlugin("com.lightbend.paradox" % "sbt-paradox-project-info" % "1.1.2")
addSbtPlugin("com.lightbend.akka" % "sbt-paradox-akka" % "0.16")
addSbtPlugin("com.eed3si9n" % "sbt-unidoc" % "0.4.2")
addSbtPlugin("com.thoughtworks.sbt-api-mappings" % "sbt-api-mappings" % "2.1.0")
addSbtPlugin("org.foundweekends" % "sbt-bintray" % "0.5.4")
addSbtPlugin("com.typesafe" % "sbt-mima-plugin" % "0.3.0")
addSbtPlugin("com.lightbend" % "sbt-whitesource" % "0.1.14")
// has following PRs merged in:
// * https://github.com/sbt/sbt-site/pull/141
// * https://github.com/sbt/sbt-site/pull/139
addSbtPlugin("com.typesafe.sbt" % "sbt-site" % "1.3.2+24-b76fdbbe")
addSbtPlugin("com.eed3si9n" % "sbt-unidoc" % "0.4.2")
addSbtPlugin("com.lightbend.sbt" % "sbt-java-formatter" % "0.4.4")
addSbtPlugin("com.lightbend.akka.grpc" % "sbt-akka-grpc" % "0.6.0")
addSbtPlugin("com.lightbend.sbt" % "sbt-javaagent" % "0.1.4")
addSbtPlugin("pl.project13.scala" % "sbt-jmh" % "0.3.4")
// depend directly on the patched version see https://github.com/akka/alpakka/issues/1388
addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.9.2+10-148ba0ff")
// patched version of sbt-dependency-graph and sbt-site
resolvers += Resolver.bintrayIvyRepo("2m", "sbt-plugins")
