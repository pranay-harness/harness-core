package migrations.all;

import static software.wings.security.PermissionAttribute.PermissionType.MANAGE_SECRETS;

import software.wings.security.PermissionAttribute.PermissionType;

import java.util.HashSet;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import migrations.accountpermission.AbstractAccountManagementPermissionMigration;

@Slf4j
public class ManageSecretsMigration extends AbstractAccountManagementPermissionMigration {
  public Set<PermissionType> getToBeAddedPermissions() {
    Set<PermissionType> toBeAddedPermissions = new HashSet<>();
    toBeAddedPermissions.add(MANAGE_SECRETS);
    return toBeAddedPermissions;
  }
}
