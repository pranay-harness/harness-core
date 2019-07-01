package io.harness.testframework.framework.utils;

import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;

import com.google.gson.JsonObject;

import io.harness.scm.ScmSecret;
import io.harness.scm.SecretName;
import io.harness.testframework.framework.constants.AccountManagementConstants.PermissionTypes;
import io.harness.testframework.restutils.UserGroupRestUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import software.wings.beans.Account;
import software.wings.beans.notification.NotificationSettings;
import software.wings.beans.notification.SlackNotificationSetting;
import software.wings.beans.security.AccountPermissions;
import software.wings.beans.security.UserGroup;
import software.wings.security.PermissionAttribute.PermissionType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
public class UserGroupUtils {
  public static UserGroup getUserGroup(Account account, String bearerToken, String groupName) {
    List<UserGroup> userGroupList = UserGroupRestUtils.getUserGroups(account, bearerToken);
    assertTrue(userGroupList != null && userGroupList.size() > 0);

    for (UserGroup elemUserGroup : userGroupList) {
      if (elemUserGroup.getName().equals(groupName)) {
        return elemUserGroup;
      }
    }
    return null;
  }

  public static boolean hasUsersInUserGroup(Account account, String bearerToken, String groupName) {
    UserGroup userGroup = getUserGroup(account, bearerToken, groupName);
    return userGroup.getMemberIds() != null && userGroup.getMemberIds().size() > 0;
  }

  public static UserGroup createUserGroup(Account account, String bearerToken, JsonObject jsonObject) {
    UserGroup userGroup = UserGroupRestUtils.createUserGroup(account, bearerToken, jsonObject);
    assertTrue(userGroup != null);
    return userGroup;
  }

  public static NotificationSettings createNotificationSettings(String emailId, String slackWebhook) {
    boolean useIndividualEmailId = false;
    List<String> emailAddresses = new ArrayList<>();
    emailAddresses.add(emailId);
    SlackNotificationSetting slackNotificationSetting = new SlackNotificationSetting("dummyChannelName", slackWebhook);
    return new NotificationSettings(useIndividualEmailId, true, emailAddresses, slackNotificationSetting, "");
  }

  public static UserGroup createUserGroupWithPermissionAndMembers(
      Account account, String bearerToken, List<String> memberIds, AccountPermissions accountPermissions) {
    JsonObject groupInfoAsJson = new JsonObject();
    String name = "UserGroup - " + System.currentTimeMillis();
    groupInfoAsJson.addProperty("name", name);
    groupInfoAsJson.addProperty("description", "Test Description - " + System.currentTimeMillis());
    UserGroup userGroup = UserGroupUtils.createUserGroup(account, bearerToken, groupInfoAsJson);
    userGroup.setMemberIds(memberIds);
    assertTrue(UserGroupRestUtils.updateMembers(account, bearerToken, userGroup) == HttpStatus.SC_OK);
    userGroup = getUserGroup(account, bearerToken, userGroup.getName());
    assertNotNull(userGroup);
    assertNotNull(userGroup.getMemberIds());
    userGroup.setAccountPermissions(accountPermissions);
    assertTrue(UserGroupRestUtils.updateAccountPermissions(account, bearerToken, userGroup) == HttpStatus.SC_OK);
    userGroup = getUserGroup(account, bearerToken, userGroup.getName());
    assertNotNull(userGroup);
    assertNotNull(userGroup.getAccountPermissions());
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
    AccountPermissions accountPermissions = AccountPermissions.builder().build();
    accountPermissions.setPermissions(permissionTypes);
    return accountPermissions;
  }

  public static void deleteMembers(Account account, String bearerToken, UserGroup userGroup) {
    List<String> emptyList = new ArrayList<>();
    userGroup.setMemberIds(emptyList);
    assertTrue(UserGroupRestUtils.updateMembers(account, bearerToken, userGroup) == HttpStatus.SC_OK);
    userGroup = UserGroupUtils.getUserGroup(account, bearerToken, userGroup.getName());
    assertTrue(userGroup.getMemberIds() == null || userGroup.getMemberIds().size() == 0);
  }

  public static UserGroup createUserGroup(
      Account account, String bearerToken, String rbacUserId, String userGroupPermission) {
    logger.info("Starting with the ReadOnly Test");
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
    } else {
      logger.warn("Unknown permission type found : " + userGroupPermission + ": proceeding with No Permission Type");
      accountPermissions = buildNoPermission();
    }
    return UserGroupUtils.createUserGroupWithPermissionAndMembers(account, bearerToken, userIds, accountPermissions);
  }

  public static UserGroup createUserGroupAndUpdateWithNotificationSettings(Account account, String bearerToken) {
    logger.info("Creating a new user group");
    JsonObject groupInfoAsJson = new JsonObject();
    String name = "UserGroup - " + System.currentTimeMillis();
    groupInfoAsJson.addProperty("name", name);
    groupInfoAsJson.addProperty("description", "Test Description - " + System.currentTimeMillis());
    UserGroup userGroup = UserGroupUtils.createUserGroup(account, bearerToken, groupInfoAsJson);
    assertNotNull(userGroup);

    logger.info("Creating a Notification Settings with an email id and slack webhook");
    String emailId = TestUtils.generateRandomUUID() + "@harness.mailinator.com";
    String slackWebHook = new ScmSecret().decryptToString(new SecretName("slack_webhook_for_alert"));
    NotificationSettings notificationSettings = UserGroupUtils.createNotificationSettings(emailId, slackWebHook);
    userGroup.setNotificationSettings(notificationSettings);
    logger.info("Update user group with notification settings");
    userGroup = UserGroupRestUtils.updateNotificationSettings(account, bearerToken, userGroup);
    assertNotNull(userGroup);
    assertNotNull(userGroup.getNotificationSettings());
    return userGroup;
  }
}
