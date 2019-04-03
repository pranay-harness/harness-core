package software.wings.service.impl.compliance;

import static software.wings.beans.Account.ACCOUNT_ID_KEY;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.mongodb.morphia.FindAndModifyOptions;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.beans.governance.GovernanceConfig;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.compliance.GovernanceConfigService;

/**
 * This implementation should only be used for LITE accounts.
 */
@Singleton
public class GovernanceConfigServiceForLite extends GovernanceConfigServiceImpl implements GovernanceConfigService {
  @Inject private WingsPersistence persistence;

  @Override
  public GovernanceConfig get(String accountId) {
    Query<GovernanceConfig> query = persistence.createQuery(GovernanceConfig.class).filter(ACCOUNT_ID_KEY, accountId);
    UpdateOperations<GovernanceConfig> updateOperations =
        persistence.createUpdateOperations(GovernanceConfig.class).set("deploymentFreeze", false);

    persistence.findAndModify(query, updateOperations, new FindAndModifyOptions());
    return new GovernanceConfig(accountId, false);
  }
}
