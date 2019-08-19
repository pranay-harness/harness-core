package software.wings.service.impl.compliance;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.EmbeddedUser;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.beans.Event.Type;
import software.wings.beans.User;
import software.wings.beans.governance.GovernanceConfig;
import software.wings.beans.governance.GovernanceConfig.GovernanceConfigKeys;
import software.wings.dl.WingsPersistence;
import software.wings.features.GovernanceFeature;
import software.wings.features.api.RestrictedApi;
import software.wings.security.UserThreadLocal;
import software.wings.service.impl.AuditServiceHelper;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.compliance.GovernanceConfigService;

import javax.annotation.Nonnull;
import javax.validation.executable.ValidateOnExecution;

/**
 * @author rktummala on 02/04/19
 */
@Slf4j
@ValidateOnExecution
@Singleton
public class GovernanceConfigServiceImpl implements GovernanceConfigService {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private AccountService accountService;
  @Inject AuditServiceHelper auditServiceHelper;

  @Override
  public GovernanceConfig get(String accountId) {
    GovernanceConfig governanceConfig =
        wingsPersistence.createQuery(GovernanceConfig.class).filter(GovernanceConfigKeys.accountId, accountId).get();
    if (governanceConfig == null) {
      return getDefaultGovernanceConfig(accountId);
    }
    return governanceConfig;
  }

  private GovernanceConfig getDefaultGovernanceConfig(String accountId) {
    return GovernanceConfig.builder().accountId(accountId).deploymentFreeze(false).build();
  }

  @Override
  @RestrictedApi(GovernanceFeature.class)
  public GovernanceConfig upsert(String accountId, @Nonnull GovernanceConfig governanceConfig) {
    GovernanceConfig oldSetting = get(accountId);

    Query<GovernanceConfig> query =
        wingsPersistence.createQuery(GovernanceConfig.class).filter(GovernanceConfigKeys.accountId, accountId);

    UpdateOperations<GovernanceConfig> updateOperations =
        wingsPersistence.createUpdateOperations(GovernanceConfig.class)
            .set(GovernanceConfigKeys.deploymentFreeze, governanceConfig.isDeploymentFreeze())
            .set(GovernanceConfigKeys.timeRangeBasedFreezeConfigs, governanceConfig.getTimeRangeBasedFreezeConfigs());

    User user = UserThreadLocal.get();
    if (null != user) {
      EmbeddedUser embeddedUser = new EmbeddedUser(user.getUuid(), user.getName(), user.getEmail());
      updateOperations.set(GovernanceConfigKeys.lastUpdatedBy, embeddedUser);
    } else {
      logger.error("ThreadLocal User is null when trying to update governance config. accountId={}", accountId);
    }

    GovernanceConfig updatedVal =
        wingsPersistence.findAndModify(query, updateOperations, WingsPersistence.upsertReturnNewOptions);
    auditServiceHelper.reportForAuditingUsingAccountId(accountId, oldSetting, updatedVal, Type.UPDATE);

    return updatedVal;
  }

  @Override
  public void deleteByAccountId(String accountId) {
    Query<GovernanceConfig> query =
        wingsPersistence.createQuery(GovernanceConfig.class).filter(GovernanceConfigKeys.accountId, accountId);
    GovernanceConfig config = query.get();
    if (wingsPersistence.delete(query)) {
      auditServiceHelper.reportDeleteForAuditingUsingAccountId(accountId, config);
    }
  }
}
