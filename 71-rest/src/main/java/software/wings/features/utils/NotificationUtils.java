package software.wings.features.utils;

import software.wings.beans.EntityType;
import software.wings.beans.security.UserGroup;
import software.wings.features.api.Usage;

public class NotificationUtils {
  private NotificationUtils() {
    throw new AssertionError();
  }

  public static Usage asUsage(UserGroup userGroup) {
    return Usage.builder()
        .entityId(userGroup.getUuid())
        .entityType(EntityType.USER_GROUP.toString())
        .entityName(userGroup.getName())
        .build();
  }
}
