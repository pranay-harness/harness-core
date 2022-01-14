package io.harness.accesscontrol.roles;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.Set;
import lombok.Builder;
import lombok.Value;
import org.hibernate.validator.constraints.NotEmpty;

@OwnedBy(HarnessTeam.PL)
@Value
@Builder
public class PrivilegedRole {
  @NotEmpty String name;
  @NotEmpty String identifier;
  @NotEmpty Set<String> permissions;
}
