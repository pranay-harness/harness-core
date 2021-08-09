package io.harness.notification.messageclient;

import io.harness.NotificationRequest;

public interface MessageClient {
  void send(NotificationRequest notificationRequest, String accountId);
}
