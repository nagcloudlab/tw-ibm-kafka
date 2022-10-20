package com.example.wiki;

import com.launchdarkly.eventsource.EventHandler;
import com.launchdarkly.eventsource.EventSource;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;

import java.net.URI;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class WikimediaChangesProducer {

    public static void main(String[] args) throws Exception {

        // Prepare a wiki
        String bootstrapServers = "127.0.0.1:9092";
        // create Producer Properties
        Properties properties = new Properties();
        properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        // set safe producer configs (Kafka <= 2.8)
        properties.setProperty(ProducerConfig.ACKS_CONFIG, "all");
        properties.setProperty(ProducerConfig.RETRIES_CONFIG,String.valueOf(Integer.MAX_VALUE));
        properties.setProperty(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG,"true");
        properties.setProperty(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION,"1");

        // set high throughput producer configs
        properties.setProperty(ProducerConfig.LINGER_MS_CONFIG,"20");
        properties.setProperty(ProducerConfig.BATCH_SIZE_CONFIG,Integer.toString(32*1024));
        properties.setProperty(ProducerConfig.COMPRESSION_TYPE_CONFIG,"lz4");


        // create the Producer
        KafkaProducer<String, String> producer = new KafkaProducer<>(properties);
        String topic="wikimedia.recentchange";

        String url = "https://stream.wikimedia.org/v2/stream/recentchange"; // event-source
        EventHandler eventHandler=new WikimediaChangeHandler(producer,topic);
        EventSource.Builder builder = new EventSource.Builder(eventHandler, URI.create(url));
        EventSource eventSource=builder.build();

        // start the producer in another thread
        eventSource.start();

        // we produce for 10 minutes and block the program until then
        TimeUnit.MINUTES.sleep(10);

    }

}
