package io.harness.migrations.accountpermission;

import static io.harness.annotations.dev.HarnessTeam.PL;

import static software.wings.security.PermissionAttribute.PermissionType.MANAGE_SSH_AND_WINRM;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.security.PermissionAttribute;

import com.google.common.collect.Sets;
import java.util.Set;

@OwnedBy(PL)
@TargetModule(HarnessModule._390_DB_MIGRATION)
public class SshAndWinRmAccountPermissionMigration extends AbstractAccountManagementPermissionMigration {
  @Override
  public Set<PermissionAttribute.PermissionType> getToBeAddedPermissions() {
    return Sets.newHashSet(MANAGE_SSH_AND_WINRM);
  }
}
