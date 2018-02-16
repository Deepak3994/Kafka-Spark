package com.ericsson.inoc.alarms.streaming.consumer

import kafka.serializer.StringDecoder

import org.apache.kafka.common.serialization.StringDeserializer

import org.apache.spark.streaming._
import org.apache.spark.SparkConf
import org.apache.spark.streaming.kafka010._
import org.apache.spark.streaming.kafka010.LocationStrategies.PreferConsistent
import org.apache.spark.streaming.kafka010.ConsumerStrategies.Subscribe

import com.ericsson.inoc.alarms.streaming.common.Alarms

/**
 * Consumes messages from one or more topics in Kafka and does wordcount.
 * Usage: CustomKafkaSparkConsumer <brokers> <topics>
 *   <brokers> is a list of one or more Kafka brokers
 *   <topics> is a list of one or more kafka topics to consume from
 *
 * Example:
 *    $ bin/run-example streaming.CustomKafkaSparkConsumer 
 */

object CustomKafkaSparkConsumer {

     

    // Create context with 2 second batch interval
	
	def main(args : Array[String]) {          
	val sparkConf = new SparkConf()
	sparkConf.setAppName("CustomKafkaSparkConsumer")
	//sparkConf.set("spark.kryo.registrator", "com.ericsson.inoc.alarms.streaming.consumer.CustomKryoRegistrator")
	sparkConf.setMaster("local[*]")
	sparkConf.set("spark.serializer","org.apache.spark.serializer.KryoSerializer")

	val ssc = new StreamingContext(sparkConf, Seconds(2))

    // Create direct kafka stream with brokers and topics
	val topics = "AlarmStreams,some-topic,any-topic,this-topic"
	//val brokers = "localhost:9092"
	val brokers = args(0)
	val topicsSet = topics.split(",").toSet
	
	val kafkaParams = Map[String, Object](
	"metadata.broker.list" -> brokers,
	"key.deserializer" -> classOf[StringDeserializer],
	"value.deserializer" -> classOf[com.ericsson.inoc.alarms.streaming.common.KryoCustSerializer],
	"bootstrap.servers" -> "localhost:9092",
	"group.id" -> "some-topic-consumer"
	)

    //StreamingExamples.setStreamingLogLevels()

    // PORTION THAT GETS EXECUTED EVERY 1 SECOND
    //BEGIN
    
	println("Dstreams got created .....")

        val kafkaMessages = KafkaUtils.createDirectStream[String, Alarms](ssc,PreferConsistent,Subscribe[String,Alarms](topicsSet, kafkaParams))
    	val alarms = kafkaMessages.map(record => (record.key, record.value))
        //kafkaMessages.print()
	alarms.print()

    //END

	ssc.start()    
	ssc.awaitTermination()
	}
}

// scalastyle:on println
