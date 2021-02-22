package io.harness.accesscontrol.roles;

import java.util.Set;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Roles {
  @NotNull Set<Role> roles;
}
