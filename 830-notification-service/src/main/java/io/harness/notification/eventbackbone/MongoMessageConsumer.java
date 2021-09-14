/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

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
  private NotificationService notificationService;

  @Inject
  public MongoMessageConsumer(
      QueueConsumer<MongoNotificationRequest> queueConsumer, NotificationService notificationService) {
    super(queueConsumer, true);
    this.notificationService = notificationService;
  }

  @Override
  public void onMessage(MongoNotificationRequest message) {
    try {
      NotificationRequest notificationRequest = NotificationRequest.parseFrom(message.getBytes());
      if (!notificationRequest.getUnknownFields().asMap().isEmpty()) {
        throw new InvalidProtocolBufferException("Unknown fields detected. Check Notification Request producer");
      }
      notificationService.processNewMessage(notificationRequest);
    } catch (InvalidProtocolBufferException e) {
      log.error("Corrupted message received off the mongo queue");
    }
  }
}
