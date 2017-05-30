organization  := "ruc.xiatian.spider"
version       := "1.3.alpha"
scalaVersion  := "2.12.1"
scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

//fork in run := true
//cancelable in Global := true

//akka
libraryDependencies +="com.typesafe.akka" %% "akka-actor" % "2.5.0"
libraryDependencies += "com.typesafe.akka" %% "akka-remote" % "2.5.0"
libraryDependencies += "com.typesafe.akka" %% "akka-slf4j" % "2.5.0"
libraryDependencies += "com.typesafe.akka" %% "akka-http" % "10.0.5"
libraryDependencies += "com.typesafe.akka" %% "akka-http-spray-json" % "10.0.5"

//akka http 跨域访问
libraryDependencies += "ch.megard" %% "akka-http-cors" % "0.2.1"

libraryDependencies += "com.typesafe" % "config" % "1.3.1"

//akka kryo serialization, it's faster than java serializer
libraryDependencies += "com.github.romix.akka" %% "akka-kryo-serialization" % "0.5.2"

//XML support
libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "1.0.6"

libraryDependencies += "org.scala-lang.modules" %% "scala-async" % "0.9.6"

//auto resource management
libraryDependencies += "com.jsuereth" %% "scala-arm" % "2.0"

//command line parser
libraryDependencies += "com.github.scopt" %% "scopt" % "3.5.0"

//Scala wrapper for Joda Time.
libraryDependencies += "com.github.nscala-time" %% "nscala-time" % "2.16.0"

//Scala better file
libraryDependencies += "com.github.pathikrit"  %% "better-files-akka"  % "3.0.0"

//Java mail
libraryDependencies += "javax.mail" % "mail"  % "1.4.7"

//bing and mongodb driver
//libraryDependencies += "org.mongodb" %% "casbah" % "3.1.1" //mongodb driver
libraryDependencies += "com.github.etaty" %% "rediscala" % "1.8.0"
libraryDependencies +=  "mysql" % "mysql-connector-java" % "5.1.23"
libraryDependencies +=  "org.reactivemongo" %% "reactivemongo" % "0.12.3"

libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.1.2"

//add jars for zhinang modules
libraryDependencies += "commons-cli" % "commons-cli" % "1.2"
libraryDependencies += "commons-codec" % "commons-codec" % "1.6"
libraryDependencies += "org.apache.commons" % "commons-lang3" % "3.3.1"
libraryDependencies += "org.apache.commons" % "commons-math3" % "3.0"
libraryDependencies += "com.google.guava" % "guava" % "18.0"
libraryDependencies += "org.bouncycastle" % "bcpg-jdk15on" % "1.55" force()

//http jars
libraryDependencies += "org.apache.httpcomponents" % "httpclient" % "4.5.2"
libraryDependencies += "org.apache.james" % "apache-mime4j-core" % "0.7.2"
libraryDependencies += "com.ibm.icu" % "icu4j" % "53.1"
libraryDependencies += "org.jsoup" % "jsoup" % "1.9.2"
libraryDependencies += "net.sourceforge.nekohtml" % "nekohtml" % "1.9.21"


//NLP libraries
libraryDependencies += "com.hankcs" % "hanlp" % "portable-1.2.11"
libraryDependencies += "org.ahocorasick" % "ahocorasick" % "0.3.0"


//libraryDependencies += "xalan" % "xalan" % "2.7.1"
//libraryDependencies += "commons-validator" % "commons-validator"  % "1.5+"


scalacOptions in Test ++= Seq("-Yrangepos")

resolvers ++= Seq(
  // other resolvers here
  // if you want to use snapshot builds (currently 0.12-SNAPSHOT), use this.
  "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/",
  "Sonatype Releases" at "https://oss.sonatype.org/content/repositories/releases/"
)

//native package
import NativePackagerHelper._
enablePlugins(JavaServerAppPackaging)
mainClass in Compile := Some("xiatian.spider.Main")
//把运行时需要的配置文件拷贝到打包后的主目录下
mappings in Universal += file("boards.xml") -> "boards.xml"
mappings in Universal += file("my.conf") -> "my.conf"
mappings in Universal += file("stopwords4article.txt") -> "stopwords4article.txt"

mappings in Universal <++= (packageBin in Compile) map { _ =>
  /**
    * 显示一个目录下的所有文件，包括文件夹中的文件，返回文件和对应的文件名称，文件名称采用相对于prefix的相对路径
    */
  def listAllFiles(root: File, prefix: String):List[(File, String)] = {
    root.listFiles().flatMap {
      f => if(f.isDirectory)
        listAllFiles(f, prefix + f.getName + "/")
      else
        List((f, prefix + f.getName))
    }.toList
  }

  listAllFiles(new File("./web"), "web/"):::listAllFiles(new File("./conf"), "conf/")
    .map{ case (f: File, path:String) => f -> path }
}

javaOptions in Universal ++= Seq(
  // -J params will be added as jvm parameters
  "-J-Xms4G",
  "-J-Xmx8G"
)


//assembly
assemblyJarName in assembly := "spider.jar"
test in assembly := {}
mainClass in assembly := Some("xiatian.spider.Main")

assemblyMergeStrategy in assembly := {
  case PathList("javax", "servlet", xs @ _*)         => MergeStrategy.first
  case PathList(ps @ _*) if ps.last endsWith ".html" => MergeStrategy.first
  case "application.conf"                            => MergeStrategy.concat
  case "logback.xml"                                 => MergeStrategy.last
  case "unwanted.txt"                                => MergeStrategy.discard
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}
