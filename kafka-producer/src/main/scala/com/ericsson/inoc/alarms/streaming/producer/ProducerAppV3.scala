package com.ericsson.inoc.alarms.streaming.producer

import java.io.File
import java.util.{UUID, Properties,Calendar}
// import com.knoldus.kafka.serializer.CustSerializer
import kafka.message.DefaultCompressionCodec
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.Serializer;

//import org.ericsson.inoc.streaming.producer.ProducerV3
import com.ericsson.inoc.alarms.streaming.common.AlarmsV3


class ProducerV3(brokerList: String) {


  private val props = new Properties()

  props.put("compression.codec", DefaultCompressionCodec.codec.toString)
  props.put("producer.type", "async")
  props.put("batch.num.messages", "200")
  props.put("metadata.broker.list", brokerList)
  
  props.put("bootstrap.servers","localhost:9092")
  
  props.put("message.send.max.retries", "5")
  props.put("request.required.acks", "-1")
  
  props.put("key.serializer","org.apache.kafka.common.serialization.StringSerializer")
  //props.put("value.serializer","org.apache.kafka.common.serialization.StringSerializer")
   props.put("value.serializer","com.ericsson.inoc.alarms.streaming.common.KryoCustSerializerV3")
  //props.put("value.serializer","com.knoldus.kafka.serializer.CustSerializer")
  //props.put("value.serializer","com.knoldus.kafka.serializer.OptionSerializers")
  props.put("client.id", UUID.randomUUID().toString())


  val producer = new KafkaProducer[String, AlarmsV3](props);

  def send(topic: String, message: AlarmsV3): Unit = send(topic, List(message))

  def send(topic: String, messages: Seq[AlarmsV3]): Unit =
    try {
      val rn = scala.util.Random
      println("sending batch messages  to kafka queue.......")
      val queueMessages = messages.map { message =>val m = new ProducerRecord[String, AlarmsV3](topic,null,null,System.currentTimeMillis.toString,message)
	 Thread.sleep(rn.nextInt(1000))
	m} 
      //val queueMessages = messages.map { message => new ProducerRecord[String, AlarmsV3](topic,null,System.currentTimeMillis,null,message) }
      
      for(qmsg <- queueMessages) {
          println(System.currentTimeMillis.toString+"  "+qmsg.value.getGenTime+"  "+qmsg.value.getMsgVal)
	  //println(qmsg.timestamp+"  "+qmsg.value.getGenTime+"  "+qmsg.value.getMsgVal)
          producer.send(qmsg)}
    } catch {
      case ex: Exception =>
        ex.printStackTrace()

    }


}

object ProducerAppV3 extends App {
  
  val topic = "Dummy-Alarm-Streams"

  val producer = new ProducerV3("localhost:9092")
  val batchSize = 100
  val r = scala.util.Random
  var cnt = 1
  while(true)
  {	
	//val now = Calendar.getInstance().getTime()
	producer.send(topic, new AlarmsV3(Calendar.getInstance().getTime,r.nextPrintableChar()))
	//producer.send(topic, new AlarmsV3(Calendar.getInstance().getTime,cnt))
	cnt = cnt + 1
	Thread.sleep(1000)
  }
}
