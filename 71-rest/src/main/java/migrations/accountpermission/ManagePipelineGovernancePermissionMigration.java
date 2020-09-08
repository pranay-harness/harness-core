package migrations.accountpermission;

import static software.wings.security.PermissionAttribute.PermissionType.MANAGE_PIPELINE_GOVERNANCE_STANDARDS;

import com.google.common.collect.Sets;

import software.wings.security.PermissionAttribute;

import java.util.Set;

public class ManagePipelineGovernancePermissionMigration extends AbstractAccountManagementPermissionMigration {
  @Override
  public Set<PermissionAttribute.PermissionType> getToBeAddedPermissions() {
    return Sets.newHashSet(MANAGE_PIPELINE_GOVERNANCE_STANDARDS);
  }
}
