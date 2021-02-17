package io.harness.accesscontrol.permissions.jobs;

import io.harness.accesscontrol.permissions.Permission;

import java.util.Set;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Permissions {
  @NotNull Set<Permission> permissions;
}
