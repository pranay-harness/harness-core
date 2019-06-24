package io.harness.testframework.framework.utils;

import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;

import com.google.gson.JsonObject;

import io.harness.testframework.restutils.UserGroupRestUtils;
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
}
