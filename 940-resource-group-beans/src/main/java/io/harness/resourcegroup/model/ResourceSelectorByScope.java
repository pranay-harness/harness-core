package io.harness.resourcegroup.model;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.Scope;

import javax.validation.Valid;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(HarnessTeam.PL)
@Data
@Builder
@TypeAlias("ResourceSelectorByScope")
public class ResourceSelectorByScope implements ResourceSelector {
  boolean includeChildScopes;
  @Valid Scope scope;
}
