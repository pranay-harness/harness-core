package io.harness.notification.eventbackbone;

import io.harness.NotificationRequest;
import io.harness.notification.entities.MongoNotificationRequest;
import io.harness.notification.service.api.NotificationService;
import io.harness.queue.QueueConsumer;
import io.harness.queue.QueueListener;

import com.google.inject.Inject;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MongoMessageConsumer extends QueueListener<MongoNotificationRequest> implements MessageConsumer {
  @Inject private NotificationService notificationService;

  @Inject
  public MongoMessageConsumer(QueueConsumer<MongoNotificationRequest> queueConsumer) {
    super(queueConsumer, true);
  }

  @Override
  public void onMessage(MongoNotificationRequest message) {
    try {
      NotificationRequest notificationRequest = NotificationRequest.parseFrom(message.getBytes());
      notificationService.processNewMessage(notificationRequest);
    } catch (InvalidProtocolBufferException e) {
      log.error("Corrupted message received off the mongo queue");
    }
  }
}
