import _root_.sbt._
import _root_.sbt.Keys._
import _root_.sbt.Resolver
import _root_.sbt.TaskKey
import _root_.sbtdocker.ImageName
import _root_.sbtdocker.mutable.Dockerfile
import AssemblyKeys._
import DockerKeys._
import sbt.Keys._
import sbtdocker.mutable.Dockerfile
import sbtdocker.ImageName
import scala.Some
import scala.Some

val _name = "stairway-to-heaven"

val _organization = "arch"

val _version = "0.1.0-SNAPSHOT"

name := _name

organization := _organization

version := _version

//homepage := Some(url("https://github.com/null/service-registry"))

publishTo := Some("Monsanto Nexus" at "http://w3.maven.monsanto.com/nexus/content/repositories/monsanto-enterprise-snapshots/")

publishMavenStyle := true

startYear := Some(2014)

/* scala versions and options */
scalaVersion := "2.11.2"

// These options will be used for *all* versions.
scalacOptions ++= Seq(
  "-deprecation"
  ,"-unchecked"
  ,"-encoding", "UTF-8"
  ,"-Xlint"
  // "-optimise"   // this option will slow your build
)

scalacOptions ++= Seq(
  "-Yclosure-elim",
  "-Yinline"
)

// These language flags will be used only for 2.10.x.
scalacOptions <++= scalaVersion map { sv =>
  if (sv startsWith "2.11") List(
    "-Xverify"
    ,"-feature"
    ,"-language:postfixOps"
  )
  else Nil
}

javacOptions ++= Seq("-Xlint:unchecked", "-Xlint:deprecation")

val akka = "2.3.3"
val spray = "1.3.2"

/* dependencies */
libraryDependencies ++= Seq (
  "com.chuusai"                 %% "shapeless"                % "2.0.0"
  //,"com.monsanto.arch"          %% "restclient"               % "0.1.0-SNAPSHOT" changing()
  //,"com.monsanto.arch"          %% "fleet-client"             % "0.1.0-SNAPSHOT" changing()
  ,"org.scala-lang.modules"     %% "scala-xml"                % "1.0.2"
  ,"com.github.nscala-time"     %% "nscala-time"              % "1.2.0"
  // -- testing --
  , "org.scalatest"             %% "scalatest"                % "2.2.1"  % "test"
  // -- Akka --
  ,"com.typesafe.akka"          %% "akka-testkit"             % akka     % "test"
  ,"com.typesafe.akka"          %% "akka-actor"               % akka
  ,"com.typesafe.akka"          %% "akka-slf4j"               % akka
  // -- Logging --
  ,"ch.qos.logback"              % "logback-classic"          % "1.1.2"
  ,"com.typesafe.scala-logging" %% "scala-logging-slf4j"      % "2.1.2"
  // -- Spray --
  ,"io.spray"                   %% "spray-routing"            % spray
  ,"io.spray"                   %% "spray-client"             % spray
  ,"io.spray"                   %% "spray-testkit"            % spray    % "test"
  ,"com.netaporter.salad"       %% "salad-metrics-core"       % "0.2.7"
  // -- metrics --
  ,"com.codahale.metrics"        % "metrics-core"             % "3.0.2"
  // -- json --
  ,"io.spray"                   %%  "spray-json"              % "1.3.1"
  // -- config --
  ,"com.typesafe"                % "config"                   % "1.2.1"
  // -- giter8 --
  ,"net.databinder.giter8"       % "giter8-lib_2.10"          % "0.6.6"
  ,"org.eclipse.jgit"            % "org.eclipse.jgit"         % "1.3.0.201202151440-r"
  ,"org.scala-lang.modules"     %% "scala-parser-combinators" % "1.0.2"
  // -- file io --
  ,"org.apache.commons"          % "commons-io"               % "1.3.2"
).map(_.force())

/* avoid duplicate slf4j bindings */
libraryDependencies ~= { _.map(_.exclude("org.slf4j", "slf4j-jdk14")) }

resolvers ++= Seq(
  "spray repo" at "http://repo.spray.io",
  "splunk repo" at "http://splunk.artifactoryonline.com/splunk/ext-releases-local",
  "Monsanto Enterprise" at "http://w3.maven.monsanto.com/nexus/content/repositories/monsanto-enterprise/",
  "Monsanto Enterprise Snapshots" at "http://w3.maven.monsanto.com/nexus/content/repositories/monsanto-enterprise-snapshots/",
  Resolver.sonatypeRepo("releases"),
  Resolver.sonatypeRepo("snapshots")
)

// ScalaStyle
org.scalastyle.sbt.ScalastylePlugin.Settings

lazy val testScalaStyle = taskKey[Unit]("testScalaStyle")

testScalaStyle := {
  org.scalastyle.sbt.PluginKeys.scalastyle.toTask("").value
}
// end ScalaStyle

//(test in Test) <<= (test in Test) dependsOn testScalaStyle

val testSettings = Seq(
  fork in Test := true,
  javaOptions in Test := Seq("-Denv=local")
)

testSettings

dockerSettings

net.virtualvoid.sbt.graph.Plugin.graphSettings

assemblySettings

mainClass in assembly := Some("com.monsanto.arch.ProjectAsAServiceApp")

test in assembly := {}

docker <<= (docker dependsOn assembly)

dockerfile in docker := {
  val artifact = (outputPath in assembly).value
  val artifactTargetPath = s"/app/${artifact.name}"
  new Dockerfile {
    from("dockerfile/java")
    add(artifact, artifactTargetPath)
    entryPoint("java", "-jar", artifactTargetPath)
    expose(80)
  }
}

imageName in docker := {
  ImageName(
    registry = Some("stluengtst01.monsanto.com:5000"),
    namespace = Some(organization.value),
    repository = name.value,
    tag = Some(version.value)
  )
}

seq(Revolver.settings: _*)

fork in run := true

javaOptions in run += "-Denv=local"


val deployTask = TaskKey[Unit]("deploy", "start an instance of this application on the fleet cluster")

deployTask := {
  (runMain in Compile).toTask(s" com.monsanto.arch.deploy.Deploy ${_name} ${_organization} ${_version}").value
}

deployTask <<= (deployTask dependsOn dockerBuildAndPush)
