package io.harness.notification.service.api;

import io.harness.NotificationRequest;
import io.harness.notification.beans.NotificationProcessingResponse;
import io.harness.notification.remote.dto.NotificationSettingDTO;

public interface ChannelService {
  NotificationProcessingResponse send(NotificationRequest notificationRequest);
  boolean sendTestNotification(NotificationSettingDTO notificationSettingDTO);
}
