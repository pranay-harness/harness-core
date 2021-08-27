package io.harness.resourcegroup.model;

import javax.validation.constraints.NotNull;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.Data;

@OwnedBy(HarnessTeam.PL)
@Data
@Builder
public class DynamicResourceSelector implements ResourceSelector {
  @NotNull String resourceType;
  Boolean includeNestedScopes;
}
