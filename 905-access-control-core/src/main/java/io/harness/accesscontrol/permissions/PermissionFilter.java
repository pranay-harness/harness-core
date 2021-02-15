package io.harness.accesscontrol.permissions;

import io.swagger.annotations.ApiModel;
import java.util.Set;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@ApiModel(value = "PermissionListQuery")
public class PermissionFilter {
  @NotNull Set<String> allowedScopeLevelsFilter;
  @NotNull Set<PermissionStatus> statusFilter;
  @NotNull Set<String> identifierFilter;

  public boolean isEmpty() {
    return allowedScopeLevelsFilter.isEmpty() && statusFilter.isEmpty() && identifierFilter.isEmpty();
  }
}
