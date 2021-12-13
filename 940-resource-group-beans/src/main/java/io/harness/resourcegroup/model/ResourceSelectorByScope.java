package io.harness.resourcegroup.model;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.Scope;

import lombok.Builder;
import lombok.Data;

@OwnedBy(HarnessTeam.PL)
@Data
@Builder
public class ResourceSelectorByScope implements ResourceSelector {
  boolean includeChildScopes;
  Scope scope;
}
