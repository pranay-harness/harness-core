/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.testframework.framework.utils;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.scm.ScmSecret;
import io.harness.scm.SecretName;
import io.harness.testframework.framework.constants.AccountManagementConstants.PermissionTypes;
import io.harness.testframework.restutils.UserGroupRestUtils;

import software.wings.beans.Account;
import software.wings.beans.notification.NotificationSettings;
import software.wings.beans.notification.SlackNotificationSetting;
import software.wings.beans.security.AccountPermissions;
import software.wings.beans.security.UserGroup;
import software.wings.security.PermissionAttribute.PermissionType;

import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;

@Slf4j
public class UserGroupUtils {
  static UserGroup getUserGroup(Account account, String bearerToken, String groupName) {
    List<UserGroup> userGroupList = UserGroupRestUtils.getUserGroups(account, bearerToken);
    assertThat(userGroupList != null && userGroupList.size() > 0).isTrue();
    if (userGroupList != null) {
      for (UserGroup elemUserGroup : userGroupList) {
        if (elemUserGroup.getName().equals(groupName)) {
          return elemUserGroup;
        }
      }
    }
    return null;
  }

  public static boolean hasUsersInUserGroup(Account account, String bearerToken, String groupName) {
    UserGroup userGroup = getUserGroup(account, bearerToken, groupName);
    if (userGroup != null) {
      return userGroup.getMemberIds() != null && userGroup.getMemberIds().size() > 0;
    }
    return false;
  }

  public static UserGroup createUserGroup(Account account, String bearerToken, JsonObject jsonObject) {
    UserGroup userGroup = UserGroupRestUtils.createUserGroup(account, bearerToken, jsonObject);
    assertThat(userGroup != null).isTrue();
    return userGroup;
  }

  public static NotificationSettings createNotificationSettings(String emailId, String slackWebhook) {
    boolean useIndividualEmailId = false;
    List<String> emailAddresses = new ArrayList<>();
    emailAddresses.add(emailId);
    SlackNotificationSetting slackNotificationSetting = new SlackNotificationSetting("dummyChannelName", slackWebhook);
    return new NotificationSettings(useIndividualEmailId, true, emailAddresses, slackNotificationSetting, "", "");
  }

  public static UserGroup createUserGroupWithPermissionAndMembers(
      Account account, String bearerToken, List<String> memberIds, AccountPermissions accountPermissions) {
    JsonObject groupInfoAsJson = new JsonObject();
    String name = "UserGroup - " + System.currentTimeMillis();
    groupInfoAsJson.addProperty("name", name);
    groupInfoAsJson.addProperty("description", "Test Description - " + System.currentTimeMillis());
    UserGroup userGroup = UserGroupUtils.createUserGroup(account, bearerToken, groupInfoAsJson);
    assertThat(userGroup).isNotNull();
    if (userGroup != null) {
      userGroup.setMemberIds(memberIds);
      assertThat(UserGroupRestUtils.updateMembers(account, bearerToken, userGroup) == HttpStatus.SC_OK).isTrue();
      userGroup = getUserGroup(account, bearerToken, userGroup.getName());
    }
    assertThat(userGroup).isNotNull();
    if (userGroup != null) {
      assertThat(userGroup.getMemberIds()).isNotNull();
      userGroup.setAccountPermissions(accountPermissions);
      assertThat(UserGroupRestUtils.updateAccountPermissions(account, bearerToken, userGroup) == HttpStatus.SC_OK)
          .isTrue();
      userGroup = getUserGroup(account, bearerToken, userGroup.getName());
    }
    assertThat(userGroup).isNotNull();
    if (userGroup != null) {
      assertThat(userGroup.getAccountPermissions()).isNotNull();
    }
    return userGroup;
  }

  public static AccountPermissions buildReadOnlyPermission() {
    Set<PermissionType> permissionTypes = new HashSet<>();
    permissionTypes.add(PermissionType.USER_PERMISSION_READ);
    AccountPermissions accountPermissions = AccountPermissions.builder().build();
    accountPermissions.setPermissions(permissionTypes);
    return accountPermissions;
  }

  public static AccountPermissions buildNoPermission() {
    Set<PermissionType> permissionTypes = new HashSet<>();
    AccountPermissions accountPermissions = AccountPermissions.builder().build();
    accountPermissions.setPermissions(permissionTypes);
    return accountPermissions;
  }

  public static AccountPermissions buildUserManagementPermission() {
    Set<PermissionType> permissionTypes = new HashSet<>();
    permissionTypes.add(PermissionType.USER_PERMISSION_READ);
    permissionTypes.add(PermissionType.USER_PERMISSION_MANAGEMENT);
    AccountPermissions accountPermissions = AccountPermissions.builder().build();
    accountPermissions.setPermissions(permissionTypes);
    return accountPermissions;
  }

  public static AccountPermissions buildAccountAdmin() {
    Set<PermissionType> permissionTypes = new HashSet<>();
    permissionTypes.add(PermissionType.USER_PERMISSION_READ);
    permissionTypes.add(PermissionType.USER_PERMISSION_MANAGEMENT);
    permissionTypes.add(PermissionType.ACCOUNT_MANAGEMENT);
    permissionTypes.add(PermissionType.MANAGE_TAGS);
    permissionTypes.add(PermissionType.MANAGE_ACCOUNT_DEFAULTS);
    permissionTypes.add(PermissionType.AUDIT_VIEWER);
    AccountPermissions accountPermissions = AccountPermissions.builder().build();
    accountPermissions.setPermissions(permissionTypes);
    return accountPermissions;
  }

  private static AccountPermissions buildAccountManagement() {
    Set<PermissionType> permissionTypes = new HashSet<>();
    permissionTypes.add(PermissionType.ACCOUNT_MANAGEMENT);
    AccountPermissions accountPermissions = AccountPermissions.builder().build();
    accountPermissions.setPermissions(permissionTypes);
    return accountPermissions;
  }

  public static void deleteMembers(Account account, String bearerToken, UserGroup userGroup) {
    List<String> emptyList = new ArrayList<>();
    userGroup.setMemberIds(emptyList);
    assertThat(UserGroupRestUtils.updateMembers(account, bearerToken, userGroup) == HttpStatus.SC_OK).isTrue();
    userGroup = UserGroupUtils.getUserGroup(account, bearerToken, userGroup.getName());
    assertThat(userGroup).isNotNull();
    if (userGroup != null) {
      assertThat(userGroup.getMemberIds() == null || userGroup.getMemberIds().size() == 0).isTrue();
    }
  }

  public static UserGroup createUserGroup(
      Account account, String bearerToken, String rbacUserId, String userGroupPermission) {
    log.info("Starting with the ReadOnly Test");
    List<String> userIds = new ArrayList<>();
    userIds.add(rbacUserId);
    AccountPermissions accountPermissions = null;
    if (userGroupPermission.equals(PermissionTypes.ACCOUNT_READONLY.toString())) {
      accountPermissions = buildReadOnlyPermission();
    } else if (userGroupPermission.equals(PermissionTypes.ACCOUNT_USERANDGROUPS.toString())) {
      accountPermissions = buildUserManagementPermission();
    } else if (userGroupPermission.equals(PermissionTypes.ACCOUNT_ADMIN.toString())) {
      accountPermissions = buildAccountAdmin();
    } else if (userGroupPermission.equals(PermissionTypes.ACCOUNT_NOPERMISSION.toString())) {
      accountPermissions = buildNoPermission();
    } else if (userGroupPermission.equals(PermissionTypes.ACCOUNT_MANAGEMENT.toString())) {
      accountPermissions = buildAccountManagement();
    } else {
      log.warn("Unknown permission type found : " + userGroupPermission + ": proceeding with No Permission Type");
      accountPermissions = buildNoPermission();
    }
    return UserGroupUtils.createUserGroupWithPermissionAndMembers(account, bearerToken, userIds, accountPermissions);
  }

  public static UserGroup createUserGroupAndUpdateWithNotificationSettings(Account account, String bearerToken) {
    log.info("Creating a new user group");
    JsonObject groupInfoAsJson = new JsonObject();
    String name = "UserGroup - " + System.currentTimeMillis();
    groupInfoAsJson.addProperty("name", name);
    groupInfoAsJson.addProperty("description", "Test Description - " + System.currentTimeMillis());
    UserGroup userGroup = UserGroupUtils.createUserGroup(account, bearerToken, groupInfoAsJson);
    assertThat(userGroup).isNotNull();

    log.info("Creating a Notification Settings with an email id and slack webhook");
    String emailId = TestUtils.generateRandomUUID() + "@harness.io";
    String slackWebHook = new ScmSecret().decryptToString(new SecretName("slack_webhook_for_alert"));
    NotificationSettings notificationSettings = UserGroupUtils.createNotificationSettings(emailId, slackWebHook);
    if (userGroup != null) {
      userGroup.setNotificationSettings(notificationSettings);
      log.info("Update user group with notification settings");
      userGroup = UserGroupRestUtils.updateNotificationSettings(account, bearerToken, userGroup);
    }
    assertThat(userGroup).isNotNull();
    if (userGroup != null) {
      assertThat(userGroup.getNotificationSettings()).isNotNull();
    }
    return userGroup;
  }
}
