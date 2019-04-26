package software.wings.service;

import static software.wings.beans.security.UserGroup.DEFAULT_ACCOUNT_ADMIN_USER_GROUP_NAME;
import static software.wings.common.Constants.DEFAULT_USER_GROUP_DESCRIPTION;

import software.wings.beans.security.UserGroup;

public class UserGroupUtils {
  public static boolean isAdminUserGroup(UserGroup userGroup) {
    if (null == userGroup || null == userGroup.getName() || null == userGroup.getDescription()) {
      return false;
    }

    return userGroup.getName().equalsIgnoreCase(DEFAULT_ACCOUNT_ADMIN_USER_GROUP_NAME)
        && userGroup.getDescription().equalsIgnoreCase(DEFAULT_USER_GROUP_DESCRIPTION) && userGroup.isDefault();
  }
}
