package software.wings.service;

import static software.wings.beans.security.UserGroup.DEFAULT_ACCOUNT_ADMIN_USER_GROUP_NAME;

import software.wings.beans.security.UserGroup;

public class UserGroupUtils {
  public static boolean isAdminUserGroup(UserGroup userGroup) {
    if (null == userGroup || null == userGroup.getName()) {
      return false;
    }

    return userGroup.getName().equalsIgnoreCase(DEFAULT_ACCOUNT_ADMIN_USER_GROUP_NAME) && userGroup.isDefault();
  }
}
