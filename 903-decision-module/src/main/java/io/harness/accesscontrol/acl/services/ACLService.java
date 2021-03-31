package io.harness.accesscontrol.acl.services;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.accesscontrol.Principal;
import io.harness.accesscontrol.acl.models.ACL;
import io.harness.accesscontrol.clients.AccessCheckResponseDTO;
import io.harness.accesscontrol.clients.PermissionCheckDTO;
import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@OwnedBy(PL)
public interface ACLService {
  AccessCheckResponseDTO checkAccess(
      @NotNull @Valid Principal principal, List<PermissionCheckDTO> permissionCheckDTOList);

  long insertAllIgnoringDuplicates(List<ACL> acls);

  long saveAll(List<ACL> acls);

  void deleteAll(List<ACL> acls);

  long deleteByRoleAssignmentId(String roleAssignmentId);

  List<ACL> getByUserGroup(String scopeIdentifier, String userGroupIdentifier);

  List<ACL> getByRole(String scopeIdentifier, String identifier, boolean managed);

  List<ACL> getByResourceGroup(String scopeIdentifier, String identifier, boolean managed);

  List<ACL> getByRoleAssignmentId(String id);
}
