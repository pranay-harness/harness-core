package io.harness.notification.service;

import io.harness.notification.NotificationChannelType;
import io.harness.notification.remote.UserGroupClient;
import io.harness.notification.service.api.NotificationSettingsService;

import com.google.inject.Inject;
import java.util.Collections;
import java.util.List;
import lombok.AllArgsConstructor;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
public class NotificationSettingsServiceImpl implements NotificationSettingsService {
  private final UserGroupClient userGroupClient;

  public List<String> getNotificationSettingsForGroups(
      List<String> userGroups, NotificationChannelType notificationChannelType, String accountId) {
    return Collections.emptyList();
  }
}
