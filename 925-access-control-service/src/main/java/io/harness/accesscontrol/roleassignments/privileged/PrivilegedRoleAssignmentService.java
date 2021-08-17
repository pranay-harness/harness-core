package io.harness.accesscontrol.roleassignments.privileged;

import io.harness.accesscontrol.principals.Principal;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.Set;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import org.hibernate.validator.constraints.NotEmpty;

@OwnedBy(HarnessTeam.PL)
public interface PrivilegedRoleAssignmentService {
  void saveAll(@NotNull @Valid Set<PrivilegedRoleAssignment> privilegedRoleAssignments);
  PrivilegedAccessResult checkAccess(@NotNull @Valid PrivilegedAccessCheck privilegedAccessCheck);
  void syncManagedGlobalRoleAssignments(@NotNull Set<Principal> principals, @NotEmpty String roleIdentifier);
  void deleteByRoleAssignment(@NotEmpty String id);
  void deleteByUserGroup(@NotEmpty String identifier, @NotEmpty String scopeIdentifier);
}
