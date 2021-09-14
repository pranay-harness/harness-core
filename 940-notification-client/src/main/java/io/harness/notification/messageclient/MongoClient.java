/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.notification.messageclient;

import io.harness.NotificationRequest;
import io.harness.notification.entities.MongoNotificationRequest;
import io.harness.queue.QueuePublisher;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class MongoClient implements MessageClient {
  @Inject QueuePublisher<MongoNotificationRequest> producer;

  @Override
  public void send(NotificationRequest notificationRequest, String accountId) {
    byte[] message = notificationRequest.toByteArray();
    producer.send(MongoNotificationRequest.builder().bytes(message).build());
  }
}
