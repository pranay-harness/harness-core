package io.harness.accesscontrol.acl.daos;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.accesscontrol.Principal;
import io.harness.accesscontrol.acl.models.ACL;
import io.harness.accesscontrol.clients.PermissionCheckDTO;
import io.harness.annotations.dev.OwnedBy;

import java.util.List;

@OwnedBy(PL)
public interface ACLDAO {
  List<ACL> get(Principal principal, List<PermissionCheckDTO> permissionsRequired);

  long saveAll(List<ACL> acls);

  void deleteAll(List<ACL> acls);

  long deleteByRoleAssignment(String roleAssignmentId);

  List<ACL> getByUserGroup(String scope, String userGroupIdentifier);

  List<ACL> getByRole(String scope, String roleIdentifier, boolean managed);

  List<ACL> getByResourceGroup(String scope, String resourceGroupIdentifier, boolean managed);

  List<ACL> getByRoleAssignment(String roleAssignmentId);
}