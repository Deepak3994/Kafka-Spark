package com.ericsson.inoc.alarms.streaming.consumer

import kafka.serializer.StringDecoder

import org.apache.kafka.common.serialization.StringDeserializer

import org.apache.spark.streaming._
import org.apache.spark.SparkConf
import org.apache.spark.streaming.kafka010._
import org.apache.spark.streaming.kafka010.LocationStrategies.PreferConsistent
import org.apache.spark.streaming.kafka010.ConsumerStrategies.Subscribe
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._
import com.ericsson.inoc.alarms.streaming.common.AlarmsV2

/**
 * Consumes messages from one or more topics in Kafka and does wordcount.
 * Usage: CustomKafkaSparkConsumerV2 <brokers> <topics>
 *   <brokers> is a list of one or more Kafka brokers
 *   <topics> is a list of one or more kafka topics to consume from
 *
 * Example:
 *    $ bin/run-example streaming.CustomKafkaSparkConsumerV2 
 */

object CustomKafkaSparkConsumerV2 {

     

    // Create context with 2 second batch interval
	
	def main(args : Array[String]) {          
	val sparkConf = new SparkConf()
	sparkConf.setAppName("CustomKafkaSparkConsumerV2")
	//sparkConf.set("spark.kryo.registrator", "com.ericsson.inoc.alarms.streaming.consumer.CustomKryoRegistrator")
	sparkConf.setMaster("local[*]")
	//sparkConf.set("spark.serializer","org.apache.spark.serializer.KryoSerializer")
	val checkpointDirectory = "/home/neo/sources/StreamingCheckPointDir"
	//val ssc = new StreamingContext(sparkConf, Seconds(2))

	//def functionToCreateContext(): StreamingContext = {
		val ssc = new StreamingContext(sparkConf, Seconds(10))   // new context
		
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

		//kafkaMessages.print()
	    	//val alarms = kafkaMessages.map(record => (record.key, record.value))
		//val alvals = kafkaMessages.map(record => record.value)
		val alvals = kafkaMessages.map(record => (record.timestamp,
							record.key.toLong,
							record.value.getGenTime.getTime,
							record.value.getMsgVal,
							(record.timestamp-record.key.toLong))
						)
		
		alvals.print()
		
		
		//val mvals = alvals.map(record => record.getMsgVal)
		val mvals = alvals.map(record => record._4)
		val sum = mvals.reduceByWindow(_+_,Seconds(10),Seconds(10))		
		val cnt = mvals.countByWindow(Seconds(10),Seconds(10))
		
		
		var sumDF : org.apache.spark.sql.Dataset[org.apache.spark.sql.Row] = null
		var cntDF : org.apache.spark.sql.Dataset[org.apache.spark.sql.Row] = null
		
		sum.foreachRDD { rdd => 
			val spark = SparkSessionSingleton.getInstance(rdd.sparkContext.getConf)
			import spark.implicits._
			sumDF = rdd.toDF("SUM")
			//sumDF.show()
		}
		cnt.foreachRDD { rdd => 
			val spark = SparkSessionSingleton.getInstance(rdd.sparkContext.getConf)
			import spark.implicits._
			cntDF = rdd.toDF("COUNT")
			val avgDF = (sumDF.first().getInt(0).toDouble/cntDF.first().getLong(0))
			println("\n\nWindowed Sum : "+sumDF.first().getInt(0) +
				 "\nWindowed Count : " +cntDF.first().getLong(0)+
				"\nWindowed Average is >>> "+avgDF+"\n\n")						
		}
		

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

object SparkSessionSingleton {

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
}

// scalastyle:on println
