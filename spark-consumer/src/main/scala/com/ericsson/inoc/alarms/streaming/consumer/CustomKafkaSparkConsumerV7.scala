package com.ericsson.inoc.alarms.streaming.consumer

import kafka.serializer.StringDecoder

import org.apache.kafka.common.serialization.StringDeserializer

import org.apache.spark.streaming._
import org.apache.spark.SparkConf
import org.apache.spark.streaming.kafka010._
import org.apache.spark.streaming.kafka010.LocationStrategies.PreferConsistent
import org.apache.spark.streaming.kafka010.ConsumerStrategies.Subscribe
import org.apache.spark.sql.SparkSession

import com.ericsson.inoc.alarms.streaming.common.AlarmsV2

/**
 * Consumes messages from one or more topics in Kafka and does wordcount.
 * Usage: CustomKafkaSparkConsumerV7 <brokers> <topics>
 *   <brokers> is a list of one or more Kafka brokers
 *   <topics> is a list of one or more kafka topics to consume from
 *
 * Example:
 *    $ bin/run-example streaming.CustomKafkaSparkConsumerV7 
 */

object CustomKafkaSparkConsumerV7 {

     

    // Create context with 2 second batch interval
	
	def main(args : Array[String]) {          
	val sparkConf = new SparkConf()
	sparkConf.setAppName("CustomKafkaSparkConsumerV7")
	sparkConf.set("spark.kryo.registrator", "com.ericsson.inoc.alarms.streaming.consumer.CustomKryoRegistrator")
	sparkConf.setMaster("local[*]")
	sparkConf.set("spark.serializer","org.apache.spark.serializer.KryoSerializer")
	val checkpointDirectory = "/home/neo/sources/StreamingCheckPointDir"
	//val ssc = new StreamingContext(sparkConf, Seconds(2))

	//def functionToCreateContext(): StreamingContext = {
		val ssc = new StreamingContext(sparkConf, Seconds(2))   // new context
		
	    // Create direct kafka stream with brokers and topics
		val topics = "some-topic,any-topic,this-topic"
		//val brokers = "localhost:9092"
		val brokers = args(0)
		val topicsSet = topics.split(",").toSet
		
		val kafkaParams = Map[String, Object](
		"metadata.broker.list" -> brokers,
		"key.deserializer" -> classOf[StringDeserializer],
		"value.deserializer" -> classOf[com.ericsson.inoc.alarms.streaming.common.KryoCustSerializerV2],
		"bootstrap.servers" -> "localhost:9092",
		"group.id" -> "some-topic-consumer"
		)

	    //StreamingExamples.setStreamingLogLevels()

	    // PORTION THAT GETS EXECUTED EVERY 1 SECOND	
	    //BEGIN
    		
		println("Dstreams got created .....")	
	        val kafkaMessages = KafkaUtils.createDirectStream[String, AlarmsV2](ssc,PreferConsistent,Subscribe[String,AlarmsV2](topicsSet, kafkaParams))

	
	    	val alarms = kafkaMessages.map(record => (record.key, record.value))
		val alvals = kafkaMessages.map(record => record.value)
		
		val mvals = alvals.map(record => record.getMsgVal)
		val sum = mvals.reduceByWindow(_+_,Seconds(10),Seconds(10))
		val sum1 = sum.map(record => (null,record))
		
		val cnt = mvals.countByWindow(Seconds(10),Seconds(10))
		val cnt1 = cnt.map(record => (null,record))
		
		val joined = sum1.join(cnt1)
		//joined.print()
		//org.apache.spark.streaming.dstream.DStream[(Null, (Int, Long))]
		
		//val winData = joined.map{def fun(x:(Null, (Int, Long))) : (Int,Long) = { x match { case ((Null,(sum, cnt))) => (sum, cnt)}}}
		
		val winData = joined.map( x => x._2._1.toDouble/x._2._2)
		winData.print()
		
		/*joined.foreachRDD{rdd => 
			//val spark = SparkSessionSingleton7.getInstance(rdd.sparkContext.getConf)
			//import spark.implicits._
			//val winDataDF = rdd.toDF("Sum","Count")
			//winDataDF.show()
			val avg = rdd.map(case(sum,count) => sum.toLong/count)
			avg.print()
		}*/
		//joined.print()
		
		//val avg = winData.map (case(a,b) => a.toFloat/b )
		//avg.print()
		
			
	    //END
		ssc.checkpoint(checkpointDirectory)   // set checkpoint directory
		//ssc

	//}

    // Get StreamingContext from checkpoint data or create a new one
	//val context = StreamingContext.getOrCreate(checkpointDirectory, functionToCreateContext _)
	//context.start()    
	//context.awaitTermination()
	ssc.start()
	ssc.awaitTermination()
	}
}
/*object SparkSessionSingleton7 {

  @transient  private var instance: SparkSession = _

  def getInstance(sparkConf: SparkConf): SparkSession = {
    if (instance == null) {
      instance = SparkSession
        .builder
        .config(sparkConf)
        .getOrCreate()
    }
    instance
  }
}*/
// scalastyle:on println
