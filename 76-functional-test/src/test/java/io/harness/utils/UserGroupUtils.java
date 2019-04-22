package io.harness.utils;

import static junit.framework.TestCase.assertTrue;

import com.google.gson.JsonObject;

import io.harness.restutils.UserGroupRestUtils;
import software.wings.beans.Account;
import software.wings.beans.notification.NotificationSettings;
import software.wings.beans.notification.SlackNotificationSetting;
import software.wings.beans.security.UserGroup;

import java.util.ArrayList;
import java.util.List;

public class UserGroupUtils {
  private static UserGroupRestUtils userGroupRestUtil = new UserGroupRestUtils();

  public static UserGroup getUserGroup(Account account, String bearerToken, String groupName) {
    List<UserGroup> userGroupList = userGroupRestUtil.getUserGroups(account, bearerToken);
    assertTrue(userGroupList != null && userGroupList.size() > 0);

    for (UserGroup elemUserGroup : userGroupList) {
      if (elemUserGroup.getName().equals(groupName)) {
        return elemUserGroup;
      }
    }
    return null;
  }

  public static UserGroup createUserGroup(Account account, String bearerToken, JsonObject jsonObject) {
    UserGroup userGroup = userGroupRestUtil.createUserGroup(account, bearerToken, jsonObject);
    assertTrue(userGroup != null);
    return userGroup;
  }

  public static NotificationSettings createNotificationSettings(String emailId, String slackWebhook) {
    boolean useIndividualEmailId = false;
    List<String> emailAddresses = new ArrayList<>();
    emailAddresses.add(emailId);
    SlackNotificationSetting slackNotificationSetting = new SlackNotificationSetting("dummyChannelName", slackWebhook);
    return new NotificationSettings(useIndividualEmailId, emailAddresses, slackNotificationSetting);
  }
}
