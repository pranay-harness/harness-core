package io.harness.accesscontrol.acl.services;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.accesscontrol.acl.models.ACL;
import io.harness.accesscontrol.clients.AccessCheckRequestDTO;
import io.harness.accesscontrol.clients.AccessCheckResponseDTO;
import io.harness.annotations.dev.OwnedBy;

import java.util.List;

@OwnedBy(PL)
public interface ACLService {
  AccessCheckResponseDTO checkAccess(
      io.harness.security.dto.Principal contextPrincipal, AccessCheckRequestDTO accessCheckRequestDTO);

  long saveAll(List<ACL> acls);

  long deleteByRoleAssignment(String roleAssignmentId);

  List<ACL> getByRoleAssignment(String roleAssignmentId);

  List<ACL> getByUserGroup(String scope, String userGroupIdentifier);

  List<ACL> getByRole(String scope, String roleIdentifier, boolean managed);

  List<ACL> getByResourceGroup(String scope, String resourceGroupIdentifier, boolean managed);
}
