package migrations.accountpermission;

import static software.wings.security.PermissionAttribute.PermissionType.MANAGE_DEPLOYMENT_FREEZES;

import com.google.common.collect.Sets;

import software.wings.security.PermissionAttribute;

import java.util.Set;

public class ManageDeploymentFreezePermissionMigration extends AbstractAccountManagementPermissionMigration {
  @Override
  public Set<PermissionAttribute.PermissionType> getToBeAddedPermissions() {
    return Sets.newHashSet(MANAGE_DEPLOYMENT_FREEZES);
  }
}
