package io.harness.migrations.accountpermission;
import static software.wings.security.PermissionAttribute.PermissionType.MANAGE_ALERT_NOTIFICATION_RULES;
import static software.wings.security.PermissionAttribute.PermissionType.MANAGE_APPLICATION_STACKS;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.security.PermissionAttribute.PermissionType;

import com.google.common.collect.Sets;
import java.util.Set;

@TargetModule(HarnessModule._390_DB_MIGRATION)
public class AlertNotificationAccountPermissionMigration extends AbstractAccountManagementPermissionMigration {
  public Set<PermissionType> getToBeAddedPermissions() {
    return Sets.newHashSet(MANAGE_ALERT_NOTIFICATION_RULES, MANAGE_APPLICATION_STACKS);
  }
}
