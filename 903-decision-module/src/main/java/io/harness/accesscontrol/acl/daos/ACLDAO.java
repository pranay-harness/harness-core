package io.harness.accesscontrol.acl.daos;

import io.harness.accesscontrol.Principal;
import io.harness.accesscontrol.acl.models.ACL;
import io.harness.accesscontrol.clients.PermissionCheckDTO;

import java.util.List;

public interface ACLDAO {
  List<ACL> get(Principal principal, List<PermissionCheckDTO> permissionsRequired);

  ACL save(ACL acl);

  long insertAllIgnoringDuplicates(List<ACL> acls);

  long saveAll(List<ACL> acls);

  void deleteAll(List<ACL> acls);

  long deleteByRoleAssignmentId(String roleAssignmentId);

  List<ACL> getByRole(String scopeIdentifier, String identifier, boolean managed);

  List<ACL> getByResourceGroup(String scopeIdentifier, String identifier, boolean managed);

  List<ACL> getByRoleAssignmentId(String id);
}