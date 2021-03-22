package io.harness.migrations.accountpermission;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.security.PermissionAttribute;

import java.util.HashSet;
import java.util.Set;

@TargetModule(HarnessModule._390_DB_MIGRATION)
public class ManageCloudProvidersAndConnectorsPermissionMigration extends AbstractAccountManagementPermissionMigration {
  @Override
  public Set<PermissionAttribute.PermissionType> getToBeAddedPermissions() {
    Set<PermissionAttribute.PermissionType> permissionTypes = new HashSet<>();
    permissionTypes.add(PermissionAttribute.PermissionType.MANAGE_CLOUD_PROVIDERS);
    permissionTypes.add(PermissionAttribute.PermissionType.MANAGE_CONNECTORS);
    return permissionTypes;
  }
}
