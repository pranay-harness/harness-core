package io.harness.resourcegroup.remote.dto;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

@OwnedBy(PL)
@Value
@Builder
public class ResourceSelectorFilter {
  @NotNull String resourceType;
  String resourceIdentifier;
}
