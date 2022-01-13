/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
