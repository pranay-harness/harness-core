package io.harness.notification.eventbackbone;

import static io.harness.NotificationClientConstants.KAFKA_TOPIC_NAME;

import io.harness.NotificationRequest;
import io.harness.notification.KafkaBackendConfiguration;
import io.harness.notification.service.api.NotificationService;
import io.harness.serializer.KafkaProtobufDeserializer;

import com.google.inject.Inject;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.Properties;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;

@Slf4j
public class KafkaMessageConsumer implements MessageConsumer {
  private final Consumer<String, NotificationRequest> consumer;
  private final NotificationService notificationService;

  @Inject
  public KafkaMessageConsumer(KafkaBackendConfiguration kafkaConfiguration, NotificationService notificationService) {
    Properties props = new Properties();
    props.put("bootstrap.servers", kafkaConfiguration.getBootstrapServers());
    props.put("security.protocol", kafkaConfiguration.getSecurityProtocol());
    props.put("sasl.jaas.config", kafkaConfiguration.getSaslJaasConfig());
    props.put("ssl.endpoint.identification.algorithm", kafkaConfiguration.getSslEndpointIdentificationAlgorithm());
    props.put("sasl.mechanism", kafkaConfiguration.getSaslMechanism());
    props.setProperty("enable.auto.commit", "false");
    props.put(ConsumerConfig.GROUP_ID_CONFIG, kafkaConfiguration.getConsumerGroupName());
    props.put(ConsumerConfig.ALLOW_AUTO_CREATE_TOPICS_CONFIG, "true");
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
    consumer = new KafkaConsumer<>(
        props, new StringDeserializer(), new KafkaProtobufDeserializer<>(NotificationRequest.parser()));
    this.notificationService = notificationService;
  }

  @Override
  public void run() {
    log.info("Staring listener...");
    TopicPartition topicPartition = new TopicPartition(KAFKA_TOPIC_NAME, 0);
    consumer.subscribe(Collections.singleton(KAFKA_TOPIC_NAME), new ConsumerRebalanceListener() {
      @Override
      public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
        throw new UnsupportedOperationException();
      }
      @Override
      public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
        // Move to the desired start offset
        // consumer.seek(topicPartition, 62L);
      }
    });

    while (true) {
      ConsumerRecords<String, NotificationRequest> records = consumer.poll(Duration.ofMillis(100));
      records.forEach(record -> {
        log.info("==================Inside vanilla consumer===============");
        log.info("==================Reading from offset {}================", record.offset());
        notificationService.processNewMessage(record.value());
      });
      consumer.commitSync();
      // log.info("====================looping===================");
    }
  }
}
