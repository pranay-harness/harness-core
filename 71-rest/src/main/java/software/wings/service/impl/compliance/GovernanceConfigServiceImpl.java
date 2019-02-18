package software.wings.service.impl.compliance;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.mongodb.morphia.query.Query;
import software.wings.beans.Base;
import software.wings.beans.governance.GovernanceConfig;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.compliance.GovernanceConfigService;

import javax.validation.executable.ValidateOnExecution;

/**
 * @author rktummala on 02/04/19
 */
@ValidateOnExecution
@Singleton
public class GovernanceConfigServiceImpl implements GovernanceConfigService {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public GovernanceConfig get(String accountId) {
    GovernanceConfig governanceConfig =
        wingsPersistence.createQuery(GovernanceConfig.class).filter(Base.ACCOUNT_ID_KEY, accountId).get();
    if (governanceConfig == null) {
      return getDefaultGovernanceConfig(accountId);
    }
    return governanceConfig;
  }

  private GovernanceConfig getDefaultGovernanceConfig(String accountId) {
    return GovernanceConfig.builder().accountId(accountId).deploymentFreeze(false).build();
  }

  @Override
  public GovernanceConfig update(String accountId, GovernanceConfig governanceConfig) {
    GovernanceConfig governanceConfigInDB = get(accountId);

    if (governanceConfig == null) {
      return governanceConfigInDB;
    }

    if (governanceConfigInDB != null) {
      governanceConfig.setUuid(governanceConfigInDB.getUuid());
      governanceConfig.setAccountId(accountId);
    }

    return wingsPersistence.saveAndGet(GovernanceConfig.class, governanceConfig);
  }

  @Override
  public void deleteByAccountId(String accountId) {
    Query<GovernanceConfig> query = wingsPersistence.createQuery(GovernanceConfig.class).filter(ACCOUNT_ID, accountId);
    wingsPersistence.delete(query);
  }
}
