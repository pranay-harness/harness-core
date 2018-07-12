package software.wings.beans.security.restrictions;

import lombok.Builder;
import lombok.Data;

import java.util.Set;

/**
 * @author rktummala on 06/24/18
 */
@Data
@Builder
public class RestrictionsSummary {
  private boolean hasAllAppAccess;
  private boolean hasAllProdEnvAccess;
  private boolean hasAllNonProdEnvAccess;
  private Set<AppRestrictionsSummary> applications;
}
