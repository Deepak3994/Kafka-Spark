package com.ericsson.inoc.alarms.streaming.producer

import java.io.File
import java.util.{UUID, Properties}
// import com.knoldus.kafka.serializer.CustSerializer
import kafka.message.DefaultCompressionCodec
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.Serializer;

//import org.ericsson.inoc.streaming.producer.Producer4
import com.ericsson.inoc.alarms.streaming.common.AlarmsV4
import com.github.tototoshi.csv._

class Producer4(brokerList: String) {


  private val props = new Properties()

  props.put("compression.codec", DefaultCompressionCodec.codec.toString)
  props.put("producer.type", "async")
  props.put("batch.num.messages", "200")
  props.put("metadata.broker.list", brokerList)
  
  props.put("bootstrap.servers","localhost:9092")
  
  props.put("message.send.max.retries", "5")
  props.put("request.required.acks", "-1")
  props.put("key.serializer","org.apache.kafka.common.serialization.StringSerializer")
  props.put("value.serializer","com.ericsson.inoc.alarms.streaming.common.KryoCustSerializerV4")
  //props.put("value.serializer","org.apache.kafka.common.serialization.StringSerializer")
  //props.put("value.serializer","com.knoldus.kafka.serializer.CustSerializer")
 
  props.put("client.id", UUID.randomUUID().toString())


  val producer = new KafkaProducer[String, AlarmsV4](props);
  val rn = scala.util.Random
  def send(topic: String, message: AlarmsV4): Unit = send(topic, List(message))

  def send(topic: String, messages: Seq[AlarmsV4]): Unit =
    try {
      println("sending batch messages  to kafka queue.......")
      val queueMessages = messages.map { message =>val m = new ProducerRecord[String, AlarmsV4](topic, message)
	Thread.sleep(rn.nextInt(500))
	m }
      
      for(qmsg <- queueMessages) {
          println(qmsg)
          producer.send(qmsg)}
    } catch {
      case ex: Exception =>
        ex.printStackTrace()

    }


}

object ProducerAppV4 extends App {
  
  val topic = "OnlineRetail"

  val producer = new Producer4("localhost:9092")
  val batchSize = 100

  val csvfile = "/home/neo/Ericsson R&D/OnlineRetail.csv"
  
  val reader = CSVReader.open(new File(csvfile))
  reader.allWithHeaders().foreach(fields => { 
  val dmsg = fields("InvoiceNo") +" | "+ fields("Description")
  //println(dmsg)
  def createMSG(sdmsg: String) : AlarmsV4 = {
      var mbers  = sdmsg.split('|')
      //val format = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm")
      //var lstOcrDateTime = format.parse(mbers(0))
      var msgobj = new AlarmsV4(System.currentTimeMillis,mbers(0),mbers(1)) 
      return msgobj;
    }
  val msg = createMSG(dmsg)
  
  producer.send(topic, msg)
  })

}
