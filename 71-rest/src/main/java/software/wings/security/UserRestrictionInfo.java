package software.wings.security;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.UsageRestrictions;
import lombok.Builder;
import lombok.Data;

import java.util.Map;
import java.util.Set;

/**
 * @author rktummala on 01/30/18
 */
@OwnedBy(PL)
@Data
@Builder
public class UserRestrictionInfo {
  private UsageRestrictions usageRestrictionsForUpdateAction;
  private Map<String, Set<String>> appEnvMapForUpdateAction;

  private UsageRestrictions usageRestrictionsForReadAction;
  private Map<String, Set<String>> appEnvMapForReadAction;
}
