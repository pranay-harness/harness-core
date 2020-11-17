package io.harness.notification.service;

import com.google.inject.Inject;
import io.harness.notification.NotificationChannelType;
import io.harness.notification.remote.UserGroupClient;
import io.harness.notification.service.api.NotificationSettingsService;
import lombok.AllArgsConstructor;

import java.util.Collections;
import java.util.List;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
public class NotificationSettingsServiceImpl implements NotificationSettingsService {
  private final UserGroupClient userGroupClient;

  public List<String> getNotificationSettingsForGroups(
      List<String> userGroups, NotificationChannelType notificationChannelType, String accountId) {
    return Collections.emptyList();
  }
}
