package migrations.accountpermission;
import static software.wings.security.PermissionAttribute.PermissionType.MANAGE_ALERT_NOTIFICATION_RULES;
import static software.wings.security.PermissionAttribute.PermissionType.MANAGE_APPLICATION_STACKS;

import com.google.common.collect.Sets;

import software.wings.security.PermissionAttribute.PermissionType;

import java.util.Set;

public class AlertNotificationAccountPermissionMigration extends AbstractAccountManagementPermissionMigration {
  public Set<PermissionType> getToBeAddedPermissions() {
    return Sets.newHashSet(MANAGE_ALERT_NOTIFICATION_RULES, MANAGE_APPLICATION_STACKS);
  }
}
