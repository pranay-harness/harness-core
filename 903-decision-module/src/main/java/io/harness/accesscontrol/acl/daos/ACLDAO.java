package io.harness.accesscontrol.acl.daos;

import io.harness.accesscontrol.Principal;
import io.harness.accesscontrol.acl.models.ACL;
import io.harness.accesscontrol.clients.PermissionCheckDTO;

import java.util.List;

public interface ACLDAO {
  List<ACL> get(Principal principal, List<PermissionCheckDTO> permissionsRequired);

  ACL save(ACL acl);

  void deleteByPrincipal(String principalType, String principalIdentifier);

  List<ACL> saveAll(List<ACL> acls);

  void deleteAll(List<ACL> acls);

  void deleteByRoleAssignmentId(String roleAssignmentId);

  List<ACL> getByRole(String scopeIdentifier, String identifier);

  List<ACL> getByResourceGroup(String scopeIdentifier, String identifier);
}