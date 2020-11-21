package io.harness.messageclient;

import io.harness.NotificationClientConstants;
import io.harness.NotificationRequest;
import io.harness.notification.KafkaBackendConfiguration;
import io.harness.serializer.KafkaProtobufSerializer;

import java.util.Objects;
import java.util.Properties;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

public class KafkaClient implements MessageClient {
  private final String topicName;
  Producer<String, NotificationRequest> producer;

  public KafkaClient(KafkaBackendConfiguration kafkaConfiguration) {
    Properties props = new Properties();
    props.put("bootstrap.servers", kafkaConfiguration.getBootstrapServers());
    props.put("security.protocol", kafkaConfiguration.getSecurityProtocol());
    props.put("sasl.jaas.config", kafkaConfiguration.getSaslJaasConfig());
    props.put("ssl.endpoint.identification.algorithm", kafkaConfiguration.getSslEndpointIdentificationAlgorithm());
    props.put("sasl.mechanism", kafkaConfiguration.getSaslMechanism());
    producer = new KafkaProducer<>(props, new StringSerializer(), new KafkaProtobufSerializer<>());
    topicName = NotificationClientConstants.KAFKA_TOPIC_NAME;
  }

  @Override
  public void send(NotificationRequest notificationRequest, String accountId) {
    Objects.requireNonNull(accountId);
    producer.send(new ProducerRecord<>(topicName, accountId, notificationRequest));
  }
}
