package io.harness.notification.service.api;

import io.harness.notification.NotificationChannelType;

import java.util.List;

public interface NotificationSettingsService {
  List<String> getNotificationSettingsForGroups(
      List<String> userGroups, NotificationChannelType notificationChannelType, String accountId);
}
