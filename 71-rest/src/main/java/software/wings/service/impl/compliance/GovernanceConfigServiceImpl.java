package software.wings.service.impl.compliance;

import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.segment.analytics.messages.TrackMessage;
import com.segment.analytics.messages.TrackMessage.Builder;
import io.harness.beans.EmbeddedUser;
import io.harness.event.handler.impl.segment.SegmentHelper;
import io.harness.event.model.EventType;
import io.harness.logging.AutoLogContext;
import io.harness.persistence.AccountLogContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ListUtils;
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
  @Inject private AuditServiceHelper auditServiceHelper;
  @Inject private SegmentHelper segmentHelper;

  @Override
  public GovernanceConfig get(String accountId) {
    try (AutoLogContext ignore = new AccountLogContext(accountId, OVERRIDE_ERROR)) {
      logger.info("Getting Deployment Freeze window");
      GovernanceConfig governanceConfig =
          wingsPersistence.createQuery(GovernanceConfig.class).filter(GovernanceConfigKeys.accountId, accountId).get();
      if (governanceConfig == null) {
        return getDefaultGovernanceConfig(accountId);
      }
      return governanceConfig;
    }
  }

  private GovernanceConfig getDefaultGovernanceConfig(String accountId) {
    return GovernanceConfig.builder().accountId(accountId).deploymentFreeze(false).build();
  }

  @Override
  @RestrictedApi(GovernanceFeature.class)
  public GovernanceConfig upsert(String accountId, @Nonnull GovernanceConfig governanceConfig) {
    try (AutoLogContext ignore = new AccountLogContext(accountId, OVERRIDE_ERROR)) {
      logger.info("Updating Deployment Freeze window");
      GovernanceConfig oldSetting = get(accountId);

      Query<GovernanceConfig> query =
          wingsPersistence.createQuery(GovernanceConfig.class).filter(GovernanceConfigKeys.accountId, accountId);

      UpdateOperations<GovernanceConfig> updateOperations =
          wingsPersistence.createUpdateOperations(GovernanceConfig.class)
              .set(GovernanceConfigKeys.deploymentFreeze, governanceConfig.isDeploymentFreeze())
              .set(GovernanceConfigKeys.timeRangeBasedFreezeConfigs, governanceConfig.getTimeRangeBasedFreezeConfigs())
              .set(GovernanceConfigKeys.weeklyFreezeConfigs, governanceConfig.getWeeklyFreezeConfigs());

      User user = UserThreadLocal.get();
      if (null != user) {
        EmbeddedUser embeddedUser = new EmbeddedUser(user.getUuid(), user.getName(), user.getEmail());
        updateOperations.set(GovernanceConfigKeys.lastUpdatedBy, embeddedUser);
      } else {
        logger.error("ThreadLocal User is null when trying to update governance config. accountId={}", accountId);
      }

      GovernanceConfig updatedSetting =
          wingsPersistence.findAndModify(query, updateOperations, WingsPersistence.upsertReturnNewOptions);
      auditDeploymentFreeze(accountId, oldSetting, updatedSetting);

      if (!ListUtils.isEqualList(
              oldSetting.getTimeRangeBasedFreezeConfigs(), governanceConfig.getTimeRangeBasedFreezeConfigs())) {
        publishToSegment(accountId, user, EventType.BLACKOUT_WINDOW_UPDATED);
      }

      if (!ListUtils.isEqualList(oldSetting.getWeeklyFreezeConfigs(), governanceConfig.getWeeklyFreezeConfigs())) {
        publishToSegment(accountId, user, EventType.BLACKOUT_WINDOW_UPDATED);
      }

      return updatedSetting;
    }
  }

  private void auditDeploymentFreeze(String accountId, GovernanceConfig oldConfig, GovernanceConfig updatedConfig) {
    if (deploymentFreezeBeingEnabled(oldConfig, updatedConfig)) {
      auditServiceHelper.reportForAuditingUsingAccountId(accountId, oldConfig, updatedConfig, Type.ENABLE);
    } else if (deploymentFreezeBeingDisabled(oldConfig, updatedConfig)) {
      auditServiceHelper.reportForAuditingUsingAccountId(accountId, oldConfig, updatedConfig, Type.DISABLE);
    } else {
      auditServiceHelper.reportForAuditingUsingAccountId(accountId, oldConfig, updatedConfig, Type.UPDATE);
    }
  }

  private boolean deploymentFreezeBeingEnabled(GovernanceConfig oldConfig, GovernanceConfig updatedConfig) {
    return updatedConfig.isDeploymentFreeze() && (oldConfig == null || !oldConfig.isDeploymentFreeze());
  }

  private boolean deploymentFreezeBeingDisabled(GovernanceConfig oldConfig, GovernanceConfig updatedConfig) {
    return !updatedConfig.isDeploymentFreeze() && (oldConfig == null || oldConfig.isDeploymentFreeze());
  }

  private void publishToSegment(String accountId, User user, EventType eventType) {
    if (null == user) {
      logger.error(
          "User is null when trying to publish to segment. Event will be skipped. Event Type: {}, accountId={}",
          eventType, accountId);
      return;
    }

    // repeating=false until repeating blackout windows are implemented
    Builder messageBuilder = TrackMessage.builder(eventType.toString())
                                 .userId(user.getUuid())
                                 .properties(new ImmutableMap.Builder<String, Object>()
                                                 .put("product", "Security")
                                                 .put("groupId", accountId)
                                                 .put("module", "Governance")
                                                 .put("repeating", false)
                                                 .build());

    segmentHelper.enqueue(messageBuilder);
  }

  @Override
  public void deleteByAccountId(String accountId) {
    try (AutoLogContext ignore = new AccountLogContext(accountId, OVERRIDE_ERROR)) {
      logger.info("Deleting Deployment Freeze window(s)");
      Query<GovernanceConfig> query =
          wingsPersistence.createQuery(GovernanceConfig.class).filter(GovernanceConfigKeys.accountId, accountId);
      GovernanceConfig config = query.get();
      if (wingsPersistence.delete(query)) {
        auditServiceHelper.reportDeleteForAuditingUsingAccountId(accountId, config);
      }
    }
  }
}
