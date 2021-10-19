package io.harness.migrations.accountpermission;

import static io.harness.annotations.dev.HarnessTeam.PL;

import static software.wings.security.PermissionAttribute.PermissionType.MANAGE_ACCOUNT_DEFAULTS;

import io.harness.annotations.dev.OwnedBy;

import software.wings.security.PermissionAttribute.PermissionType;

import com.google.common.collect.Sets;
import java.util.Set;

@OwnedBy(PL)
public class ManageAccountDefaultsPermissionMigration extends AbstractAccountManagementPermissionMigration {
  @Override
  public Set<PermissionType> getToBeAddedPermissions() {
    return Sets.newHashSet(MANAGE_ACCOUNT_DEFAULTS);
  }
}
