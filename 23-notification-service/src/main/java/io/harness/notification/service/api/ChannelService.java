package io.harness.notification.service.api;

import io.harness.NotificationRequest;
import io.harness.notification.remote.dto.NotificationSettingDTO;

public interface ChannelService {
  boolean send(NotificationRequest notificationRequest);
  boolean sendTestNotification(NotificationSettingDTO notificationSettingDTO);
}
