package software.wings.beans.security.restrictions;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.Data;

import java.util.Set;

/**
 * @author rktummala on 06/24/18
 */
@OwnedBy(PL)
@Data
@Builder
public class RestrictionsSummary {
  private boolean hasAllAppAccess;
  private boolean hasAllProdEnvAccess;
  private boolean hasAllNonProdEnvAccess;
  private Set<AppRestrictionsSummary> applications;
}
