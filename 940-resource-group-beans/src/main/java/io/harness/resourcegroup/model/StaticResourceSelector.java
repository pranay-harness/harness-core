package io.harness.resourcegroup.model;

import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StaticResourceSelector implements ResourceSelector {
  @NotNull String identifier;
  @NotNull String resourceType;
}
