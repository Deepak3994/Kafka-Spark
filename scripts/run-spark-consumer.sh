cd /home/neo/sources/inoc-streaming/spark-consumer/
sbt clean package
/home/neo/tools/spark-2.1.0-bin-hadoop2.7/bin/spark-submit --class "com.ericsson.inoc.alarms.streaming.consumer.CustomKafkaSparkConsumer" --jars=/home/neo/sources/inoc-streaming/spark-consumer/target/scala-2.11/inoc-spark-consumer_2.11-1.0.jar,/home/neo/.ivy2/cache/org.apache.kafka/kafka_2.11/jars/kafka_2.11-0.10.0.0.jar,/home/neo/.ivy2/cache/com.yammer.metrics/metrics-core/jars/metrics-core-2.2.0.jar,/home/neo/.ivy2/cache/com.esotericsoftware/kryo/bundles/kryo-4.0.0.jar,/home/neo/.ivy2/cache/org.scala-lang/scala-library/jars/scala-library-2.11.8.jar,/home/neo/sources/inoc-streaming/kafka-common/target/scala-2.11/inoc-kafka-common_2.11-1.0.jar,/home/neo/.ivy2/cache/org.objenesis/objenesis/jars/objenesis-2.2.jar,/home/neo/.ivy2/cache/com.esotericsoftware.minlog/minlog/jars/minlog-1.2.jar,/home/neo/.ivy2/cache/org.apache.spark/spark-streaming_2.11/jars/spark-streaming_2.11-2.1.0.jar,/home/neo/.ivy2/cache/org.apache.spark/spark-streaming-kafka-0-10_2.11/jars/spark-streaming-kafka-0-10_2.11-2.1.0.jar /home/neo/.ivy2/cache/org.apache.kafka/kafka-clients/jars/kafka-clients-0.10.0.0.jar localhost:9092


#/home/neo/.ivy2/cache/org.slf4j/slf4j-api/jars/slf4j-api-1.7.21.jar,
