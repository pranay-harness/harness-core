package software.wings.service.intfc.compliance;

import software.wings.beans.governance.GovernanceConfig;
import software.wings.service.intfc.ownership.OwnedByAccount;

/**
 * @author rktummala on 02/04/19
 */
public interface GovernanceConfigService extends OwnedByAccount {
  GovernanceConfig get(String accountId);

  GovernanceConfig update(String accountId, GovernanceConfig governanceConfig);
}
