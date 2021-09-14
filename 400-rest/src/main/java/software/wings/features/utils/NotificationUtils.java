/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.features.utils;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.EntityType;
import software.wings.beans.security.UserGroup;
import software.wings.features.api.Usage;

@OwnedBy(PL)
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
