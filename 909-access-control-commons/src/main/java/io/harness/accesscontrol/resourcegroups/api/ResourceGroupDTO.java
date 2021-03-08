package io.harness.accesscontrol.resourcegroups.api;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.hibernate.validator.constraints.NotEmpty;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ResourceGroupDTO {
  @NotEmpty String identifier;
  @NotEmpty String name;
}
