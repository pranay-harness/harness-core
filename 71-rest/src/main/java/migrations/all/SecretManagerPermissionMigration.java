package migrations.all;

import static software.wings.security.PermissionAttribute.PermissionType.MANAGE_SECRET_MANAGERS;

import lombok.extern.slf4j.Slf4j;
import migrations.accountpermission.AbstractAccountManagementPermissionMigration;
import software.wings.security.PermissionAttribute.PermissionType;

import java.util.HashSet;
import java.util.Set;

@Slf4j
public class SecretManagerPermissionMigration extends AbstractAccountManagementPermissionMigration {
  public Set<PermissionType> getToBeAddedPermissions() {
    Set<PermissionType> toBeAddedPermissions = new HashSet<>();
    toBeAddedPermissions.add(MANAGE_SECRET_MANAGERS);
    return toBeAddedPermissions;
  }
}