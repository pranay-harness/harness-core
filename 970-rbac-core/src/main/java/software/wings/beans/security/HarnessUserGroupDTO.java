package software.wings.beans.security;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.Set;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(HarnessTeam.PL)
public class HarnessUserGroupDTO {
  private String name;
  private String description;
  private Set<String> emailIds;
  private Set<String> accountIds;
}
