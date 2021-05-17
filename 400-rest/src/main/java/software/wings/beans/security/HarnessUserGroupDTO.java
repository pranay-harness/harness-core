package software.wings.beans.security;

import static io.harness.annotations.dev.HarnessModule._970_RBAC_CORE;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import java.util.Set;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(HarnessTeam.PL)
@TargetModule(_970_RBAC_CORE)
public class HarnessUserGroupDTO {
  private String name;
  private String description;
  private Set<String> emailIds;
  private Set<String> accountIds;
}
