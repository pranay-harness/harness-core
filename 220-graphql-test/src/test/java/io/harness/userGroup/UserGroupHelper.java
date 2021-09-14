/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.userGroup;

import software.wings.beans.notification.NotificationSettings;
import software.wings.beans.security.AccountPermissions;
import software.wings.beans.security.AppPermission;
import software.wings.beans.security.UserGroup;
import software.wings.service.intfc.UserGroupService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

@Singleton
public class UserGroupHelper {
  @Inject UserGroupService userGroupService;
  UserGroup createUserGroupWithPermissions(
      String accountId, AccountPermissions accountPermissions, Set<AppPermission> appPermissions) {
    UserGroup userGroup = UserGroup.builder()
                              .accountId(accountId)
                              .description("Test UserGroup")
                              .name("AccountPermission-UserGroup-" + System.currentTimeMillis())
                              .accountPermissions(accountPermissions)
                              .appPermissions(appPermissions)
                              .build();
    return userGroupService.save(userGroup);
  }

  public UserGroup createUserGroup(String accountId, String name, String description) {
    UserGroup userGroup = UserGroup.builder()
                              .accountId(accountId)
                              .name(name)
                              .description(description)
                              .isSsoLinked(false)
                              .importedByScim(false)
                              .notificationSettings(getNotificationSettings())
                              .build();
    return userGroupService.save(userGroup);
  }

  NotificationSettings getNotificationSettings() {
    return new NotificationSettings(false, true, Arrays.asList("abc@example.com"), null, null, "");
  }

  public UserGroup createUserGroupWithUsers(String accountId, String name, String description, List<String> userIds) {
    UserGroup userGroup = UserGroup.builder()
                              .accountId(accountId)
                              .isSsoLinked(false)
                              .name(name)
                              .description(description)
                              .notificationSettings(getNotificationSettings())
                              .memberIds(userIds)
                              .importedByScim(false)
                              .build();
    return userGroupService.save(userGroup);
  }
}
