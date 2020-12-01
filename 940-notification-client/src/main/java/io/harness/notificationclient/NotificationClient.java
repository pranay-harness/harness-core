package io.harness.notificationclient;

import io.harness.channeldetails.NotificationChannel;
import io.harness.notification.NotificationResult;
import io.harness.notification.remote.dto.NotificationSettingDTO;

import java.util.List;

public interface NotificationClient {
  NotificationResult sendNotificationAsync(NotificationChannel notificationChannel);
  List<NotificationResult> sendBulkNotificationAsync(List<NotificationChannel> notificationChannels);
  boolean testNotificationChannel(NotificationSettingDTO notificationSettingDTO);
}
