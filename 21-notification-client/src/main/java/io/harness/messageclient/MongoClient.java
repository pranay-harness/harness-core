package io.harness.messageclient;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.harness.NotificationRequest;
import io.harness.ng.MongoNotificationRequest;
import io.harness.queue.QueuePublisher;

@Singleton
public class MongoClient implements MessageClient {
  @Inject QueuePublisher<MongoNotificationRequest> producer;

  @Override
  public void send(NotificationRequest notificationRequest, String accountId) {
    byte[] message = notificationRequest.toByteArray();
    producer.send(MongoNotificationRequest.builder().bytes(message).build());
  }
}
