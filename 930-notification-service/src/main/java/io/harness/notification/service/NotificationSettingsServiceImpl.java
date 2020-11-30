package io.harness.notification.service;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.remote.client.NGRestUtils.getResponse;

import io.harness.ng.core.dto.NotificationSettingConfigDTO;
import io.harness.ng.core.dto.UserGroupDTO;
import io.harness.ng.core.user.User;
import io.harness.ng.core.user.remote.UserClient;
import io.harness.notification.NotificationChannelType;
import io.harness.notification.remote.UserGroupClient;
import io.harness.notification.service.api.NotificationSettingsService;
import io.harness.remote.client.RestClientUtils;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class NotificationSettingsServiceImpl implements NotificationSettingsService {
  private final UserGroupClient userGroupClient;
  private final UserClient userClient;

  private List<UserGroupDTO> getUserGroups(List<String> userGroupIds) {
    if (isEmpty(userGroupIds)) {
      return new ArrayList<>();
    }
    List<UserGroupDTO> userGroups = new ArrayList<>();
    try {
      userGroups = getResponse(userGroupClient.getUserGroups(userGroupIds));
    } catch (Exception ex) {
      log.error("Error while fetching user groups.", ex);
    }
    return userGroups;
  }

  private List<String> getEmailsForUserIds(List<String> userIds) {
    if (isEmpty(userIds)) {
      return new ArrayList<>();
    }
    List<User> users = new ArrayList<>();
    try {
      users = RestClientUtils.getResponse(userClient.getUsersByIds(userIds));
    } catch (Exception exception) {
      log.error("Failure while fetching emails of users from userIds", exception);
    }
    return users.stream().map(User::getEmail).collect(Collectors.toList());
  }

  public List<String> getNotificationSettingsForGroups(
      List<String> userGroupIds, NotificationChannelType notificationChannelType, String accountId) {
    // get user groups by ids
    List<UserGroupDTO> userGroups = getUserGroups(userGroupIds);

    Set<String> notificationSettings = new HashSet<>();
    for (UserGroupDTO userGroupDTO : userGroups) {
      for (NotificationSettingConfigDTO notificationSettingConfigDTO : userGroupDTO.getNotificationConfigs()) {
        if (notificationSettingConfigDTO.getType() == notificationChannelType
            && notificationSettingConfigDTO.getSetting().isPresent()) {
          notificationSettings.add(notificationSettingConfigDTO.getSetting().get());
        } else if (notificationChannelType == NotificationChannelType.EMAIL) {
          notificationSettings.addAll(getEmailsForUserIds(userGroupDTO.getUsers()));
        }
      }
    }
    return Lists.newArrayList(notificationSettings);
  }
}
