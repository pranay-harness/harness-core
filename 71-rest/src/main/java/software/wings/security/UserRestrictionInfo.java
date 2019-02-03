package software.wings.security;

import lombok.Builder;
import lombok.Data;
import software.wings.settings.UsageRestrictions;

import java.util.Map;
import java.util.Set;

/**
 * @author rktummala on 01/30/18
 */
@Data
@Builder
public class UserRestrictionInfo {
  private UsageRestrictions usageRestrictionsForUpdateAction;
  private Map<String, Set<String>> appEnvMapForUpdateAction;

  private UsageRestrictions usageRestrictionsForReadAction;
  private Map<String, Set<String>> appEnvMapForReadAction;
}
