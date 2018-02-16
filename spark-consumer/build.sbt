name := """inoc-spark-consumer"""

version := "1.0"

scalaVersion := "2.11.8"
val sparkVersion = "2.1.0"
//val sparkVersion = "2.0.1"
val kryoVersion = "4.0.0"

libraryDependencies ++= Seq(
	"org.apache.spark" %% "spark-streaming" % sparkVersion,
	"org.apache.spark" %% "spark-streaming-kafka-0-10" % sparkVersion,	
        "com.esotericsoftware" % "kryo" % kryoVersion,
        "com.ericsson.inoc.alarms.streaming.common" % "inoc-kafka-common" % "1.0" from "file:///home/neo/sources/inoc-streaming/kafka-common/target/scala-2.11/inoc-kafka-common_2.11-1.0.jar",
	"org.apache.spark" % "spark-sql_2.11" % sparkVersion,
	"org.apache.spark" % "spark-core_2.11" % sparkVersion,
	"org.apache.spark" % "spark-mllib_2.11" % sparkVersion

)

