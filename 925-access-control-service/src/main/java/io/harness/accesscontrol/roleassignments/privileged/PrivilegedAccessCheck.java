package io.harness.accesscontrol.roleassignments.privileged;

import io.harness.accesscontrol.acl.PermissionCheck;
import io.harness.accesscontrol.principals.Principal;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;
import org.hibernate.validator.constraints.NotEmpty;

@OwnedBy(HarnessTeam.PL)
@Value
@Builder
public class PrivilegedAccessCheck {
  @NotEmpty String accountIdentifier;
  @NotNull Principal principal;
  @NotNull List<PermissionCheck> permissionChecks;
}
