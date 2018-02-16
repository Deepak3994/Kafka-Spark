package com.ericsson.inoc.alarms.streaming.consumer

import kafka.serializer.StringDecoder
import org.apache.kafka.common.serialization.StringDeserializer
import kafka.message.DefaultCompressionCodec
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import org.apache.spark.streaming._
import org.apache.spark.SparkConf
import org.apache.spark.streaming.kafka010._
import org.apache.spark.streaming.kafka010.LocationStrategies.PreferConsistent
import org.apache.spark.streaming.kafka010.ConsumerStrategies.Subscribe

/**
 * Consumes messages from one or more topics in Kafka and does wordcount.
 * Usage: CustomKafkaSparkConsumerV5 <brokers> <topics>
 *   <brokers> is a list of one or more Kafka brokers
 *   <topics> is a list of one or more kafka topics to consume from
 *
 * Example:
 *    $ bin/run-example streaming.CustomKafkaSparkConsumerV5 
 */

object CustomKafkaSparkConsumerV5 {

    // Create context with 2 second batch interval
	
	def main(args : Array[String]) {          
	val sparkConf = new SparkConf()
	sparkConf.setAppName("CustomKafkaSparkConsumerV5")
	sparkConf.set("spark.kryo.registrator", "com.ericsson.inoc.alarms.streaming.consumer.CustomKryoRegistrator")
	sparkConf.setMaster("local[*]")
	sparkConf.set("spark.serializer","org.apache.spark.serializer.KryoSerializer")
	val checkpointDirectory = "/home/neo/sources/StreamingCheckPointDir/ConsumerV5"
	//val ssc = new StreamingContext(sparkConf, Seconds(2))

		val ssc = new StreamingContext(sparkConf, Seconds(2))   // new context
		
	    // Create direct kafka stream with brokers and topics
		val topics = "cntTopic,any-topic,this-topic"
		//val brokers = "localhost:9092"
		val brokers = args(0)
		val topicsSet = topics.split(",").toSet
		
		val kafkaParams = Map[String, Object](
		"metadata.broker.list" -> brokers,
		"key.deserializer" -> classOf[StringDeserializer],
		"value.deserializer" -> classOf[StringDeserializer],
		"bootstrap.servers" -> "localhost:9092",
		"group.id" -> "some-topic-consumer"
		)

	    //StreamingExamples.setStreamingLogLevels()

	    // PORTION THAT GETS EXECUTED EVERY 1 SECOND	
	    //BEGIN
    		
		println("Dstreams got created .....")	
	        val kafkaMessages = KafkaUtils.createDirectStream[String, String](ssc,PreferConsistent,Subscribe[String,String](topicsSet, kafkaParams))

	
	    	val alarms = kafkaMessages.map(record => (record.key, record.value))
		val alvals = kafkaMessages.map(record => record.value)
		
		alvals.print()
		
	    //END
		ssc.checkpoint(checkpointDirectory)   // set checkpoint directory

	ssc.start()
	ssc.awaitTermination()
	}
}

