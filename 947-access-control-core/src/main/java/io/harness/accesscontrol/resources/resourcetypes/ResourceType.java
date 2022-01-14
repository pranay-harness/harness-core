package io.harness.accesscontrol.resources.resourcetypes;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.hibernate.validator.constraints.NotEmpty;

@OwnedBy(HarnessTeam.PL)
@Value
@Builder
@EqualsAndHashCode
public class ResourceType {
  @NotEmpty String identifier;
  @NotEmpty String permissionKey;
}
