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
  PrivilegedAccessResult checkAccess(@NotNull @Valid PrivilegedAccessCheck privilegedAccessCheck);
  void syncManagedGlobalRoleAssignments(@NotNull Set<Principal> principals, @NotEmpty String roleIdentifier);
}
