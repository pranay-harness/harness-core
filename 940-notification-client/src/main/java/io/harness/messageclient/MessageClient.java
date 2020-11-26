package io.harness.messageclient;

import io.harness.NotificationRequest;

public interface MessageClient {
  public void send(NotificationRequest notificationRequest, String accountId);
}
