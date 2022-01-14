package io.harness.migrations.accountpermission;

import software.wings.security.PermissionAttribute;

import java.util.HashSet;
import java.util.Set;

public class ManageCloudProvidersAndConnectorsPermissionMigration extends AbstractAccountManagementPermissionMigration {
  @Override
  public Set<PermissionAttribute.PermissionType> getToBeAddedPermissions() {
    Set<PermissionAttribute.PermissionType> permissionTypes = new HashSet<>();
    permissionTypes.add(PermissionAttribute.PermissionType.MANAGE_CLOUD_PROVIDERS);
    permissionTypes.add(PermissionAttribute.PermissionType.MANAGE_CONNECTORS);
    return permissionTypes;
  }
}
