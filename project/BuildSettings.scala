import sbt._
import Keys._
import com.typesafe.sbt.SbtPgp
import com.typesafe.sbt.SbtScalariform
import com.typesafe.sbt.SbtScalariform.ScalariformKeys
import sbtassembly.Plugin.AssemblyKeys._
import sbtassembly.Plugin._
import spray.revolver.RevolverPlugin.Revolver
import com.typesafe.sbt.osgi.SbtOsgi
import SbtOsgi._

object BuildSettings {
  val VERSION = "1.3.4-elastic-cloud-SNAPSHOT"

  lazy val basicSettings = seq(
    version               := NightlyBuildSupport.buildVersion(VERSION),
    homepage              := Some(new URL("http://spray.io")),
    organization          := "io.spray",
    organizationHomepage  := Some(new URL("http://spray.io")),
    description           := "A suite of lightweight Scala libraries for building and consuming RESTful " +
                             "web services on top of Akka",
    startYear             := Some(2011),
    licenses              := Seq("Apache 2" -> new URL("http://www.apache.org/licenses/LICENSE-2.0.txt")),
    scalaVersion          := "2.11.8",
//    crossScalaVersions    := Seq("2.11.8", "2.10.6"),
    resolvers             ++= Dependencies.resolutionRepos,
    scalacOptions         := Seq(
      "-encoding", "utf8",
      "-feature",
      "-unchecked",
      "-deprecation",
      "-target:jvm-1.6",
      "-language:_",
      "-Xlog-reflective-calls"
    )
  )

  lazy val sprayModuleSettings =
    basicSettings ++ formatSettings ++
    NightlyBuildSupport.settings ++
    SbtPgp.settings ++
    Seq(
      // scaladoc settings
      (scalacOptions in doc) <++= (name, version).map { (n, v) => Seq("-doc-title", n, "-doc-version", v) },

      // publishing
      crossPaths := true,
      publishMavenStyle := true,
      SbtPgp.useGpg := true,
      credentials += Credentials(sbt.Path.userHome / ".sbt" / ".found-credentials"),
      publishTo <<= version { version =>
        if (version.trim.endsWith("SNAPSHOT")) {
          Some("Cloud snapshots" at "https://artifactory.found.no/artifactory/libs-snapshot-local")
        } else {
          Some("Cloud releases" at "https://artifactory.found.no/artifactory/libs-release-local")
        }
      },
      pomIncludeRepository := { _ => false },
      pomExtra :=
        <scm>
          <url>git://github.com/spray/spray.git</url>
          <connection>scm:git:git@github.com:spray/spray.git</connection>
        </scm>
        <developers>
          <developer><id>sirthias</id><name>Mathias Doenitz</name></developer>
          <developer><id>jrudolph</id><name>Johannes Rudolph</name></developer>
        </developers>
    )

  lazy val noPublishing = seq(
    publish := (),
    publishLocal := (),
    // required until these tickets are closed https://github.com/sbt/sbt-pgp/issues/42,
    // https://github.com/sbt/sbt-pgp/issues/36
    publishTo := None
  )

  lazy val generateSprayVersionConf = TaskKey[Seq[File]]("generate-spray-version-conf",
    "Create a reference.conf file in the managed resources folder that contains a spray.version = ... setting")

  lazy val sprayVersionConfGeneration = seq(
    (unmanagedResources in Compile) <<= (unmanagedResources in Compile).map(_.filter(_.getName != "reference.conf")),
    resourceGenerators in Compile <+= generateSprayVersionConf,
    generateSprayVersionConf <<= (unmanagedResourceDirectories in Compile, resourceManaged in Compile, version) map {
      (sourceDir, targetDir, version) => {
        val source = sourceDir / "reference.conf"
        val target = targetDir / "reference.conf"
        val conf = IO.read(source.get.head)
        IO.write(target, conf.replace("<VERSION>", version))
        Seq(target)
      }
    }
  )

  lazy val docsSettings = basicSettings ++ noPublishing ++ seq(
    unmanagedSourceDirectories in Test <<= baseDirectory { _ ** "code" get }
  )

  lazy val exampleSettings = basicSettings ++ noPublishing ++ seq(Dependencies.scalaXmlModule)
  lazy val standaloneServerExampleSettings = exampleSettings ++ Revolver.settings

  lazy val benchmarkSettings = basicSettings ++ noPublishing ++ Revolver.settings ++ assemblySettings ++ Seq(
    Dependencies.scalaXmlModule,
    mainClass in assembly := Some("spray.examples.Main"),
    jarName in assembly := "benchmark.jar",
    test in assembly := {},
    javaOptions in Revolver.reStart ++= Seq("-verbose:gc", "-XX:+PrintCompilation")
  )

  lazy val jettyExampleSettings = exampleSettings ++ com.earldouglas.xwp.XwpPlugin.jetty()

  lazy val formatSettings = SbtScalariform.scalariformSettings ++ Seq(
    ScalariformKeys.preferences in Compile := formattingPreferences,
    ScalariformKeys.preferences in Test    := formattingPreferences
  )

  import scalariform.formatter.preferences._
  def formattingPreferences =
    FormattingPreferences()
      .setPreference(RewriteArrowSymbols, true)
      .setPreference(AlignParameters, true)
      .setPreference(AlignSingleLineCaseStatements, true)
      .setPreference(DoubleIndentClassDeclaration, true)

  def osgiSettings(exports: Seq[String], imports: Seq[String] = Seq.empty) =
    SbtOsgi.osgiSettings ++ Seq(
      OsgiKeys.exportPackage := exports map { pkg => pkg + ".*;version=\"${Bundle-Version}\"" },
      OsgiKeys.importPackage <<= scalaVersion { sv =>
        Seq(CrossVersion.partialVersion(sv) match {
        case Some((2, scalaMinor)) if scalaMinor >= 11 =>
          """scala.xml.*;version="$<range;[==,=+);1.0.2>",scala.*;version="$<range;[==,=+);%s>""""
        case _ =>
          """scala.*;version="$<range;[==,=+);%s>""""
        }) map (_.format(sv))
      },
      OsgiKeys.importPackage ++= imports,
      OsgiKeys.importPackage += "akka.spray.*;version=\"${Bundle-Version}\"",
      OsgiKeys.importPackage += """akka.*;version="$<range;[==,=+)>"""",
      OsgiKeys.importPackage += "*",
      OsgiKeys.additionalHeaders := Map("-removeheaders" -> "Include-Resource,Private-Package")
    )

}
