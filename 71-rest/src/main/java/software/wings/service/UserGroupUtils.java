package software.wings.service;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static software.wings.beans.security.UserGroup.DEFAULT_ACCOUNT_ADMIN_USER_GROUP_NAME;

import io.harness.annotations.dev.OwnedBy;
import lombok.experimental.UtilityClass;
import software.wings.beans.security.UserGroup;

@OwnedBy(PL)
@UtilityClass
public class UserGroupUtils {
  public static boolean isAdminUserGroup(UserGroup userGroup) {
    if (null == userGroup || null == userGroup.getName()) {
      return false;
    }

    return userGroup.getName().equalsIgnoreCase(DEFAULT_ACCOUNT_ADMIN_USER_GROUP_NAME) && userGroup.isDefault();
  }
}
