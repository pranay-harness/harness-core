package io.harness.notification.notificationclient;

import io.harness.Team;
import io.harness.notification.NotificationResult;
import io.harness.notification.channeldetails.NotificationChannel;
import io.harness.notification.remote.dto.NotificationSettingDTO;
import io.harness.notification.remote.dto.TemplateDTO;
import io.harness.notification.templates.PredefinedTemplate;

import java.util.List;

public interface NotificationClient {
  NotificationResult sendNotificationAsync(NotificationChannel notificationChannel);
  List<NotificationResult> sendBulkNotificationAsync(List<NotificationChannel> notificationChannels);
  boolean testNotificationChannel(NotificationSettingDTO notificationSettingDTO);
  TemplateDTO saveNotificationTemplate(Team team, PredefinedTemplate template, Boolean harnessManaged);
}
