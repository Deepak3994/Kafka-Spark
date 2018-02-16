package com.ericsson.inoc.alarms.streaming.consumer

import org.apache.spark.serializer.KryoRegistrator;
import com.esotericsoftware.kryo.Kryo;
import com.ericsson.inoc.alarms.streaming.common.Alarms
class CustomKryoRegistrator extends KryoRegistrator {
  override def registerClasses(kryo: Kryo) {
    kryo.register(classOf[Alarms], new com.ericsson.inoc.alarms.streaming.common.KryoInternalSerializer)
  } 
}
