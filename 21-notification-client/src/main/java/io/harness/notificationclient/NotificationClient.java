package io.harness.notificationclient;

import io.harness.channeldetails.NotificationChannel;
import io.harness.notification.remote.dto.NotificationSettingDTO;
import io.harness.notification.NotificationResult;

public interface NotificationClient {
  NotificationResult sendNotificationAsync(NotificationChannel notificationChannel);
  boolean testNotificationChannel(NotificationSettingDTO notificationSettingDTO);
}
