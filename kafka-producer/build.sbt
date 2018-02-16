name := """inoc-kafka-producer"""

version := "1.0"

scalaVersion := "2.11.8"

val kryoVersion = "4.0.0"


libraryDependencies ++= Seq(
  "org.apache.kafka" % "kafka_2.11" % "0.10.0.0",
  "com.github.tototoshi" %% "scala-csv" % "1.3.4",
  "com.esotericsoftware" % "kryo" % "4.0.0",
  "com.ericsson.inoc.alarms.streaming.common" % "inoc-kafka-common" % "1.0" from "file:///home/neo/sources/inoc-streaming/kafka-common/target/scala-2.11/inoc-kafka-common_2.11-1.0.jar"
)

