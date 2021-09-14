/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.notification.service.api;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.NotificationRequest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.notification.NotificationChannelType;
import io.harness.notification.SmtpConfig;
import io.harness.notification.entities.NotificationSetting;
import io.harness.notification.remote.SmtpConfigResponse;

import java.util.List;
import java.util.Optional;

@OwnedBy(PL)
public interface NotificationSettingsService {
  List<String> getNotificationRequestForUserGroups(List<NotificationRequest.UserGroup> notificationUserGroups,
      NotificationChannelType notificationChannelType, String accountId);
  List<String> getNotificationSettingsForGroups(
      List<String> userGroups, NotificationChannelType notificationChannelType, String accountId);
  Optional<NotificationSetting> getNotificationSetting(String accountId);
  boolean getSendNotificationViaDelegate(String accountId);
  Optional<SmtpConfig> getSmtpConfig(String accountId);
  NotificationSetting setSendNotificationViaDelegate(String accountId, boolean sendNotificationViaDelegate);
  NotificationSetting setSmtpConfig(String accountId, SmtpConfig smtpConfig);

  SmtpConfigResponse getSmtpConfigResponse(String accountId);
}
