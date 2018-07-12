package software.wings.beans.security.restrictions;

import lombok.Builder;
import lombok.Data;
import software.wings.beans.EntityReference;

import java.util.Set;

/**
 * @author rktummala on 06/24/18
 */
@Data
@Builder
public class AppRestrictionsSummary {
  private EntityReference application;
  private boolean hasAllProdEnvAccess;
  private boolean hasAllNonProdEnvAccess;
  private Set<EntityReference> environments;
}
