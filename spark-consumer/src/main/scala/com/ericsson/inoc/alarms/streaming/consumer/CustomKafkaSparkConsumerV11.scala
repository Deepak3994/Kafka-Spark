package com.ericsson.inoc.alarms.streaming.consumer

import com.ericsson.inoc.alarms.streaming.common.Alarms

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
 * Usage: CustomKafkaSparkConsumerV11 <brokers> <topics>
 *   <brokers> is a list of one or more Kafka brokers
 *   <topics> is a list of one or more kafka topics to consume from
 *
 * Example:
 *    $ bin/run-example streaming.CustomKafkaSparkConsumerV11 
 */

object CustomKafkaSparkConsumerV11 {

     

    // Create context with 2 second batch interval
	
	def main(args : Array[String]) {          
	val sparkConf = new SparkConf()
	sparkConf.setAppName("CustomKafkaSparkConsumerV11")
	//sparkConf.set("spark.kryo.registrator", "com.ericsson.inoc.alarms.streaming.consumer.CustomKryoRegistrator")
	sparkConf.setMaster("local[*]")
	//sparkConf.set("spark.serializer","org.apache.spark.serializer.KryoSerializer")
	val checkpointDirectory = "/home/neo/sources/StreamingCheckPointDir"
	//val ssc = new StreamingContext(sparkConf, Seconds(2))

	//def functionToCreateContext(): StreamingContext = {
		val ssc = new StreamingContext(sparkConf, Seconds(10))   // new context
		
	    // Create direct kafka stream with brokers and topics
		val topics = "AlarmStreams,Dummy-Alarm-Streams,any-topic,this-topic"
		//val brokers = "localhost:9092"
		val brokers = args(0)
		val topicsSet = topics.split(",").toSet
		
		val kafkaParams = Map[String, Object](
		"metadata.broker.list" -> brokers,
		"key.deserializer" -> classOf[StringDeserializer],
		"value.deserializer" -> classOf[com.ericsson.inoc.alarms.streaming.common.KryoCustSerializer],
		"bootstrap.servers" -> "localhost:9092",
		"group.id" -> "AlarmStreams-consumer"
		
		)

	    //StreamingExamples.setStreamingLogLevels()

	    // PORTION THAT GETS EXECUTED EVERY 1 SECOND	
	    //BEGIN
    		
		println("Dstreams got created .....")	
	        val kafkaMessages = KafkaUtils.createDirectStream[String, Alarms](ssc,
                                                                                  PreferConsistent,
                                                                                  Subscribe[String,Alarms](topicsSet,
                                                                                                           kafkaParams))
		val alarmData = kafkaMessages.map(record => (record.value.getNode,
                                                             record.value.getNodeType,
                                                             record.value.getX733sp))
		//alarmData.print()
		val itmDstr = alarmData.map(record => record._2+"<$>"+record._3) // Eg : R1 <$> LinkFail
		//itmDstr.print(50)
		val trDstr = itmDstr.window(Seconds(10),Seconds(10)) // Transaction Window, Wi = Ti : {A1,A2,A3,A2, ...}
		//trDstr.print(25)
		//val trDstrStr = trDstr.map{ x => x.toString}

                //PFP-Growth works with Array[Transactions] where Transactions = Array[items] and items = Alarm Objects
                // Dstreams = RDD[string] needs to be converted to RDD[Array[items]]
		var trBuf = new ArrayBuffer[String] 
		val redunTrDstr = trDstr.map(x => { 
					     trBuf += x //Adding each RDD element of windowed stream to empty ArrayBuffer
					     var trArr = trBuf 
                                              /*Emitting tuple (ArrayValue,ListValue) as dstream of Array[items]
                                             (Array['A1'],List('A1'))
                                             (Array['A1','A2'],List('A1','A2'))
                                             (Array['A1','A2','A3'],Array('A1','A2','A3'))*/
				             (trArr.toArray,trArr.toList)})
		//redunTrDstr.print()

                // Union will result into distinct single Array[items=alarms]
		val arrayBasedTrs = redunTrDstr.map(_._1).cache()
		val uniTrDstr = arrayBasedTrs.reduce(_.union(_).distinct)
                val listBasedTrs = redunTrDstr.map(_._2).reduce(_.union(_).distinct).cache()
		listBasedTrs.print(30) //Prints Transactions upto 30 
		val listWinTrs = listBasedTrs.window(Seconds(300),Seconds(300)) //PFP-Window for printing
		val mngWinDstr = uniTrDstr.window(Seconds(300),Seconds(300)) // For PFP-Growth input = Descrete Set of Transactions
		listWinTrs.print()
		//mngWinDstr.print(200)

                //PFP-Growth for each PFP-Window			
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

