name := "wikidigger"
version := "0.1"

scalaVersion := "2.12.3"
sbtVersion := "1.2.1"
scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")


lazy val root = (project in file(".")).
  enablePlugins(BuildInfoPlugin).
  settings(
    buildInfoKeys := BuildInfoKey.ofN(name, version, scalaVersion, sbtVersion),
    buildInfoPackage := "wiki.digger.common"
  )

buildInfoKeys ++= Seq[BuildInfoKey](
  "author" -> "XiaTian"
)

buildInfoKeys += buildInfoBuildNumber
buildInfoOptions += BuildInfoOption.BuildTime
buildInfoOptions += BuildInfoOption.ToJson

//akka
val akkaVersion = "2.5.14"

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % akkaVersion
libraryDependencies += "com.typesafe.akka" %% "akka-remote" % akkaVersion
libraryDependencies += "com.typesafe.akka" %% "akka-cluster" % akkaVersion
libraryDependencies += "com.typesafe.akka" %% "akka-slf4j" % akkaVersion

libraryDependencies += "com.typesafe.akka" %% "akka-stream" % akkaVersion

libraryDependencies += "com.typesafe.akka" %% "akka-http" % "10.1.0"
libraryDependencies += "com.typesafe.akka" %% "akka-http-spray-json" % "10.1.0"


//akka http 跨域访问
libraryDependencies += "ch.megard" %% "akka-http-cors" % "0.3.0"

libraryDependencies += "com.typesafe" % "config" % "1.3.3"

//akka kryo serialization, it's faster than java serializer
//libraryDependencies += "com.github.romix.akka" %% "akka-kryo-serialization" % "0.5.2"

//XML support
libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "1.0.6"

//command line parser
libraryDependencies += "com.github.scopt" %% "scopt" % "3.5.0"

//Scala wrapper for Joda Time.
libraryDependencies += "com.github.nscala-time" %% "nscala-time" % "2.16.0"

//Scala better file
libraryDependencies += "com.github.pathikrit" %% "better-files-akka" % "3.0.0"


//bing and mongodb driver
libraryDependencies += "org.postgresql" % "postgresql" % "42.2.2"
libraryDependencies += "org.rocksdb" % "rocksdbjni" % "5.7.2"


libraryDependencies ++= Seq(
  "com.typesafe.slick" %% "slick" % "3.2.2",
  "com.typesafe.slick" %% "slick-hikaricp" % "3.2.2"
)


libraryDependencies += "com.github.tminglei" %% "slick-pg" % "0.16.0"
libraryDependencies += "com.github.tminglei" %% "slick-pg_joda-time" % "0.16.0"
libraryDependencies += "com.github.tminglei" %% "slick-pg_spray-json" % "0.16.0"
libraryDependencies += "com.github.tminglei" %% "slick-pg_jts" % "0.16.0"


libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.1.2"

//add jars for zhinang modules
//libraryDependencies += "commons-cli" % "commons-cli" % "1.2"
libraryDependencies += "commons-codec" % "commons-codec" % "1.6"
libraryDependencies += "org.apache.commons" % "commons-lang3" % "3.3.1"
libraryDependencies += "org.apache.commons" % "commons-math3" % "3.0"
libraryDependencies += "com.google.guava" % "guava" % "24.0-jre"

//HTTP
libraryDependencies += "org.jsoup" % "jsoup" % "1.11.2"

//NLP libraries
libraryDependencies += "com.hankcs" % "hanlp" % "portable-1.2.11"
libraryDependencies += "org.ahocorasick" % "ahocorasick" % "0.3.0"

//JWPL
libraryDependencies += "de.tudarmstadt.ukp.wikipedia" % "de.tudarmstadt.ukp.wikipedia.api" % "1.1.0"
libraryDependencies += "de.tudarmstadt.ukp.wikipedia" % "de.tudarmstadt.ukp.wikipedia.datamachine" % "1.1.0"
libraryDependencies += "de.tudarmstadt.ukp.wikipedia" % "de.tudarmstadt.ukp.wikipedia.util" % "1.1.0"
libraryDependencies += "de.tudarmstadt.ukp.wikipedia" % "de.tudarmstadt.ukp.wikipedia.parser" % "1.1.0"

//CIRCE JSON Parser
libraryDependencies ++= Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser"
).map(_ % "0.9.1")



//Scala Test library
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.5" % "test"

resolvers += "Sonatype Releases" at "https://oss.sonatype.org/content/repositories/releases/"

scalacOptions in Test ++= Seq("-Yrangepos")


import com.typesafe.sbt.SbtNativePackager.autoImport.NativePackagerHelper._
//enablePlugins(JavaServerAppPackaging)
enablePlugins(JavaAppPackaging)

mainClass in Compile := Some("xiatian.wiki.Start")

mappings in(Compile, packageDoc) := Seq()

//把运行时需要的配置文件拷贝到打包后的主目录下
//mappings in Universal += file("my.conf") -> "my.conf"
mappings in Universal ++= directory("web")
mappings in Universal ++= directory("conf")


javaOptions in Universal ++= Seq(
  // -J params will be added as jvm parameters
  "-J-Xms2G",
  "-J-Xmx4G"
)

//解决windows的line too long问题
scriptClasspath := Seq("*")


initialCommands in console +=
  """
    |import akka.actor._
    |import akka.routing._
    |
  """.stripMargin

