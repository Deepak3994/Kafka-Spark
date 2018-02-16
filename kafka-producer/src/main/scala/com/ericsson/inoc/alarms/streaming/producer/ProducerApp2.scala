package com.ericsson.inoc.alarms.streaming.producer

import java.io.File
import java.util.{UUID, Properties}
// import com.knoldus.kafka.serializer.CustSerializer
import kafka.message.DefaultCompressionCodec
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.Serializer;

//import org.ericsson.inoc.streaming.producer.Producer2
import com.ericsson.inoc.alarms.streaming.common.Alarms
import com.github.tototoshi.csv._

class Producer2(brokerList: String) {


  private val props = new Properties()

  props.put("compression.codec", DefaultCompressionCodec.codec.toString)
  props.put("producer.type", "async")
  props.put("batch.num.messages", "200")
  props.put("metadata.broker.list", brokerList)
  
  props.put("bootstrap.servers","localhost:9092")
  
  props.put("message.send.max.retries", "5")
  props.put("request.required.acks", "-1")
  props.put("key.serializer","org.apache.kafka.common.serialization.StringSerializer")
  props.put("value.serializer","com.ericsson.inoc.alarms.streaming.common.KryoCustSerializer")
  //props.put("value.serializer","org.apache.kafka.common.serialization.StringSerializer")
  //props.put("value.serializer","com.knoldus.kafka.serializer.CustSerializer")
 
  props.put("client.id", UUID.randomUUID().toString())


  val producer = new KafkaProducer[String, Alarms](props);
  val rn = scala.util.Random
  def send(topic: String, message: Alarms): Unit = send(topic, List(message))

  def send(topic: String, messages: Seq[Alarms]): Unit =
    try {
      println("sending batch messages  to kafka queue.......")
      val queueMessages = messages.map { message => val m = new ProducerRecord[String, Alarms](topic, message)
			Thread.sleep(rn.nextInt(500))
			m }
      
      for(qmsg <- queueMessages) {
          println(qmsg.value)
          producer.send(qmsg)}
    } catch {
      case ex: Exception =>
        ex.printStackTrace()

    }


}

object ProducerApp2 extends App {
  
  val topic = "AlarmStreams"

  val producer = new Producer2("localhost:9092")
  val batchSize = 100

  val csvfile = "/home/neo/alarm-grouper/src/main/resources/data/Alarms-9thSept2015.csv"
  //val csvfile = "/home/neo/sources/localrun7/sampleData.csv"
  val reader = CSVReader.open(new File(csvfile))
  reader.allWithHeaders().foreach(fields => { 
  val dmsg = fields("LASTOCCURRENCE") +"|"+ fields("NODE") +"|"+ fields("NODETYPE") +"|"+ fields ("X733SPECIFICPROB")
  
  def createMSG(sdmsg: String) : Alarms = {
      var mbers  = sdmsg.split('|')
      val format = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm")
      var lstOcrDateTime = format.parse(mbers(0))
      var msgobj = new Alarms(lstOcrDateTime,mbers(1),mbers(2),mbers(3)) 
      return msgobj;
    }
  val msg = createMSG(dmsg)
  producer.send(topic, msg)
  })

}
