package software.wings.settings;

import software.wings.security.UsageRestrictions;

import java.util.Map;
import java.util.Set;
import lombok.Builder;
import lombok.Data;

/**
 * This is a wrapper class that needs
 * @author rktummala on 07/26/18
 */
@Data
@Builder
public class RestrictionsAndAppEnvMap {
  private UsageRestrictions usageRestrictions;
  private Map<String, Set<String>> appEnvMap;
}
