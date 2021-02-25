package io.harness.accesscontrol.resources.resourcetypes;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.hibernate.validator.constraints.NotEmpty;

@Value
@Builder
@EqualsAndHashCode
public class ResourceType {
  @NotEmpty String identifier;
  @NotEmpty String permissionKey;
}
