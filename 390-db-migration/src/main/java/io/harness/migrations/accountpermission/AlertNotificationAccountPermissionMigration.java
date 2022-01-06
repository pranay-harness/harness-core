/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.migrations.accountpermission;
import static software.wings.security.PermissionAttribute.PermissionType.MANAGE_ALERT_NOTIFICATION_RULES;
import static software.wings.security.PermissionAttribute.PermissionType.MANAGE_APPLICATION_STACKS;

import software.wings.security.PermissionAttribute.PermissionType;

import com.google.common.collect.Sets;
import java.util.Set;

public class AlertNotificationAccountPermissionMigration extends AbstractAccountManagementPermissionMigration {
  public Set<PermissionType> getToBeAddedPermissions() {
    return Sets.newHashSet(MANAGE_ALERT_NOTIFICATION_RULES, MANAGE_APPLICATION_STACKS);
  }
}
