/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
