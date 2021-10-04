package io.harness.resourcegroup;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.Set;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;
import org.hibernate.validator.constraints.NotEmpty;

@OwnedBy(HarnessTeam.PL)
@Value
@Builder
public class ResourceGroupsConfig {
  @NotEmpty String name;
  int version;
  @NotNull Set<ResourceGroupConfig> resourceGroups;
}
