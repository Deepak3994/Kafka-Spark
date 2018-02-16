package com.ericsson.inoc.alarms.streaming.consumer

import com.ericsson.inoc.alarms.streaming.common.AlarmsV4

import kafka.serializer.StringDecoder
import org.apache.kafka.common.serialization.StringDeserializer

import org.apache.spark.streaming._
import org.apache.spark.SparkConf
import org.apache.spark.streaming.kafka010._
import org.apache.spark.streaming.kafka010.LocationStrategies.PreferConsistent
import org.apache.spark.streaming.kafka010.ConsumerStrategies.Subscribe
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._
import org.apache.spark.mllib.fpm.FPGrowth
import org.apache.spark.rdd.RDD

import scala.collection.mutable.ArrayBuffer

/**
 * Consumes messages from one or more topics in Kafka and does wordcount.
 * Usage: CustomKafkaSparkConsumerV10 <brokers> <topics>
 *   <brokers> is a list of one or more Kafka brokers
 *   <topics> is a list of one or more kafka topics to consume from
 *
 * Example:
 *    $ bin/run-example streaming.CustomKafkaSparkConsumerV10 
 */

object CustomKafkaSparkConsumerV10 {

     

    // Create context with 2 second batch interval
	
	def main(args : Array[String]) {          
	val sparkConf = new SparkConf()
	sparkConf.setAppName("CustomKafkaSparkConsumerV10")
	//sparkConf.set("spark.kryo.registrator", "com.ericsson.inoc.alarms.streaming.consumer.CustomKryoRegistrator")
	sparkConf.setMaster("local[*]")
	//sparkConf.set("spark.serializer","org.apache.spark.serializer.KryoSerializer")
	val checkpointDirectory = "/home/neo/sources/StreamingCheckPointDir"
	//val ssc = new StreamingContext(sparkConf, Seconds(2))

	//def functionToCreateContext(): StreamingContext = {
		val ssc = new StreamingContext(sparkConf, Seconds(10))   // new context
		
	    // Create direct kafka stream with brokers and topics
		val topics = "OnlineRetail,Dummy-Alarm-Streams,any-topic,this-topic"
		//val brokers = "localhost:9092"
		val brokers = args(0)
		val topicsSet = topics.split(",").toSet
		
		val kafkaParams = Map[String, Object](
		"metadata.broker.list" -> brokers,
		"key.deserializer" -> classOf[StringDeserializer],
		"value.deserializer" -> classOf[com.ericsson.inoc.alarms.streaming.common.KryoCustSerializerV4],
		"bootstrap.servers" -> "localhost:9092",
		"group.id" -> "some-topic-consumer"
		
		)

	    //StreamingExamples.setStreamingLogLevels()

	    // PORTION THAT GETS EXECUTED EVERY 1 SECOND	
	    //BEGIN
    		
		println("Dstreams got created .....")	
	        val kafkaMessages = KafkaUtils.createDirectStream[String, AlarmsV4](ssc,PreferConsistent,Subscribe[String,AlarmsV4](topicsSet, kafkaParams))

		val alarmData = kafkaMessages.map(record => (record.value.getGenTime,record.value.getInvoiceNo,record.value.getItem))
		
		//alarmData.print()
		
		val itmDstr = alarmData.map(record => record._3)
		//itmDstr.print(50)
		val trDstr = itmDstr.window(Seconds(10),Seconds(10))
		//trDstr.print(25)
		//val trDstrStr = trDstr.map{ x => x.toString}
		var trBuf = new ArrayBuffer[String]
		val redunTrDstr = trDstr.map(x => { 
					trBuf += x
					var trArr = trBuf
				(trArr.toArray,trArr.toList)})
		//redunTrDstr.print()
		val listBasedTrs = redunTrDstr.map(_._2).reduce(_.union(_).distinct).cache()
		val arrayBasedTrs = redunTrDstr.map(_._1).cache()
		val uniTrDstr = arrayBasedTrs.reduce(_.union(_).distinct)
		listBasedTrs.print(30)
		val listWinTrs = listBasedTrs.window(Seconds(60),Seconds(60))
		val mngWinDstr = uniTrDstr.window(Seconds(60),Seconds(60))
		listWinTrs.print()
		//mngWinDstr.print(200)				
		mngWinDstr.foreachRDD{ rdd =>
			//val trns = rdd.take(rdd.count.toInt)
			//val trs = rdd.map(s => s.trim.split(' '))
			val fpg = new FPGrowth()
					  .setMinSupport(0.3)
					  .setNumPartitions(10)
			val model = fpg.run(rdd)
			model.freqItemsets.collect().foreach { itemset =>
				  println(itemset.items.mkString("[", ",", "]") + ", " + itemset.freq)
			}	
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

