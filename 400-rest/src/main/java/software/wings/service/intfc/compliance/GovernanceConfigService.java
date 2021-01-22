package software.wings.service.intfc.compliance;

import io.harness.governance.DeploymentFreezeInfo;

import software.wings.beans.governance.GovernanceConfig;
import software.wings.service.intfc.ownership.OwnedByAccount;

/**
 * @author rktummala on 02/04/19
 */
public interface GovernanceConfigService extends OwnedByAccount {
  GovernanceConfig get(String accountId);

  GovernanceConfig upsert(String accountId, GovernanceConfig governanceConfig);

  /**
   * @param accountId the accountId
   * @return Returns a deployment freeze info object consisting information about master deployment freeze status,
   *     frozen apps and environments
   */
  DeploymentFreezeInfo getDeploymentFreezeInfo(String accountId);
}
