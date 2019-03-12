package io.harness.Utils;

import static junit.framework.TestCase.assertTrue;

import io.harness.RestUtils.UserGroupRestUtil;
import software.wings.beans.Account;
import software.wings.beans.security.UserGroup;

import java.util.List;

public class UserGroupUtils {
  private static UserGroupRestUtil userGroupRestUtil = new UserGroupRestUtil();

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
}
