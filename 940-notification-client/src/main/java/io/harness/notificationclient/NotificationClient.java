package io.harness.notificationclient;

import io.harness.channeldetails.NotificationChannel;
import io.harness.notification.NotificationResult;
import io.harness.notification.remote.dto.NotificationSettingDTO;

public interface NotificationClient {
  NotificationResult sendNotificationAsync(NotificationChannel notificationChannel);
  boolean testNotificationChannel(NotificationSettingDTO notificationSettingDTO);
}
