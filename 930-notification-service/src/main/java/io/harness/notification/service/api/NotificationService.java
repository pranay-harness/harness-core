package io.harness.notification.service.api;

import io.harness.NotificationRequest;
import io.harness.notification.entities.Notification;

public interface NotificationService {
  boolean processNewMessage(NotificationRequest notificationRequest);

  void processRetries(Notification notification);
}
