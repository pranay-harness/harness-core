package io.harness.aggregator.services.apis;

import io.harness.accesscontrol.acl.models.ACL;
import io.harness.accesscontrol.resources.resourcegroups.persistence.ResourceGroupDBO;
import io.harness.accesscontrol.roleassignments.persistence.RoleAssignmentDBO;
import io.harness.accesscontrol.roles.persistence.RoleDBO;

import java.util.List;
import javax.validation.constraints.NotNull;
import org.hibernate.validator.constraints.NotEmpty;

public interface AggregatorService {
  List<ACL> processRoleAssignmentCreation(@NotNull RoleAssignmentDBO roleAssignmentDBO);

  void processRoleAssignmentDeletion(@NotEmpty String roleAssignmentId);

  List<ACL> processRoleAssignmentUpdation(@NotNull RoleAssignmentDBO roleAssignmentDBO);

  List<ACL> processRoleUpdation(@NotNull RoleDBO roleDBO);

  List<ACL> processResourceGroupUpdation(@NotNull ResourceGroupDBO resourceGroupDBO);
}
