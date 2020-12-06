package software.wings.beans.security.restrictions;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.EntityReference;

import java.util.Set;
import lombok.Builder;
import lombok.Data;

/**
 * @author rktummala on 06/24/18
 */
@OwnedBy(PL)
@Data
@Builder
public class AppRestrictionsSummary {
  private EntityReference application;
  private boolean hasAllProdEnvAccess;
  private boolean hasAllNonProdEnvAccess;
  private Set<EntityReference> environments;
}
