package io.harness.notification.service.api;

import io.harness.notification.NotificationChannelType;
import io.harness.notification.SmtpConfig;
import io.harness.notification.entities.NotificationSetting;

import java.util.List;
import java.util.Optional;

public interface NotificationSettingsService {
  List<String> getNotificationSettingsForGroups(
      List<String> userGroups, NotificationChannelType notificationChannelType, String accountId);
  Optional<NotificationSetting> getNotificationSetting(String accountId);
  boolean getSendNotificationViaDelegate(String accountId);
  Optional<SmtpConfig> getSmtpConfig(String accountId);
  NotificationSetting setSendNotificationViaDelegate(String accountId, boolean sendNotificationViaDelegate);
  NotificationSetting setSmtpConfig(String accountId, SmtpConfig smtpConfig);
}
