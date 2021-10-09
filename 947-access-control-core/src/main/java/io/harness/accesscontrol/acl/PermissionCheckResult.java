package io.harness.accesscontrol.acl;

import io.harness.accesscontrol.scopes.core.Scope;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Value;
import org.hibernate.validator.constraints.NotEmpty;

@OwnedBy(HarnessTeam.PL)
@Value
@Builder
public class PermissionCheckResult {
  Scope resourceScope;
  @NotEmpty String resourceType;
  String resourceIdentifier;
  @NotEmpty String permission;
  boolean permitted;
}
