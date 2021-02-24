package io.harness.resourcegroup.model;

import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

@Data
@Builder
@FieldNameConstants(innerTypeName = "StaticResourceSelectorKeys")
public class StaticResourceSelector implements ResourceSelector {
  @NotNull String identifier;
  @NotNull String resourceType;
}
