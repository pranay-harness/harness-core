package software.wings.service.impl.compliance;

import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;

import io.harness.beans.EmbeddedUser;
import io.harness.beans.FeatureName;
import io.harness.data.structure.EmptyPredicate;
import io.harness.event.handler.impl.segment.SegmentHelper;
import io.harness.event.model.EventType;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.ff.FeatureFlagService;
import io.harness.governance.BlackoutWindowFilterType;
import io.harness.governance.CustomAppFilter;
import io.harness.governance.EnvironmentFilter.EnvironmentFilterType;
import io.harness.governance.GovernanceFreezeConfig;
import io.harness.governance.TimeRangeBasedFreezeConfig;
import io.harness.logging.AccountLogContext;
import io.harness.logging.AutoLogContext;

import software.wings.beans.Event.Type;
import software.wings.beans.User;
import software.wings.beans.governance.GovernanceConfig;
import software.wings.beans.governance.GovernanceConfig.GovernanceConfigKeys;
import software.wings.dl.WingsPersistence;
import software.wings.features.GovernanceFeature;
import software.wings.features.api.AccountId;
import software.wings.features.api.RestrictedApi;
import software.wings.security.UserThreadLocal;
import software.wings.service.impl.AuditServiceHelper;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.compliance.GovernanceConfigService;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.segment.analytics.messages.TrackMessage;
import com.segment.analytics.messages.TrackMessage.Builder;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.validation.executable.ValidateOnExecution;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ListUtils;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

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
  @Inject private FeatureFlagService featureFlagService;

  @Override
  public GovernanceConfig get(String accountId) {
    try (AutoLogContext ignore = new AccountLogContext(accountId, OVERRIDE_ERROR)) {
      log.info("Getting Deployment Freeze window");
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
  public GovernanceConfig upsert(@AccountId String accountId, @Nonnull GovernanceConfig governanceConfig) {
    try (AutoLogContext ignore = new AccountLogContext(accountId, OVERRIDE_ERROR)) {
      log.info("Updating Deployment Freeze window");
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
        log.error("ThreadLocal User is null when trying to update governance config. accountId={}", accountId);
      }

      if (featureFlagService.isEnabled(FeatureName.TIME_RANGE_FREEZE_GOVERNANCE_DEV, accountId)) {
        validateDeploymentFreezeInput(governanceConfig.getTimeRangeBasedFreezeConfigs());
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

  private void validateDeploymentFreezeInput(List<TimeRangeBasedFreezeConfig> timeRangeBasedFreezeConfigs) {
    if (EmptyPredicate.isEmpty(timeRangeBasedFreezeConfigs)) {
      return;
    }

    Set<String> freezeNameSet = new HashSet<>();
    timeRangeBasedFreezeConfigs.stream().map(GovernanceFreezeConfig::getName).filter(Objects::nonNull).forEach(name -> {
      if (freezeNameSet.contains(name)) {
        throw new InvalidRequestException(
            String.format("Duplicate Deployment Freeze name %s found.", name), WingsException.USER);
      }
      freezeNameSet.add(name);
    });
    timeRangeBasedFreezeConfigs.stream()
        .filter(freeze -> EmptyPredicate.isNotEmpty(freeze.getAppSelections()))
        .forEach(deploymentFreeze -> {
          if (deploymentFreeze.getAppSelections().stream().anyMatch(appSelection
                  -> appSelection.getFilterType() != BlackoutWindowFilterType.CUSTOM
                      && appSelection.getEnvSelection().getFilterType() == EnvironmentFilterType.CUSTOM)) {
            throw new InvalidRequestException(
                "Environment filter type can be CUSTOM only when Application Filter type is CUSTOM");
          }
          if (deploymentFreeze.getAppSelections()
                  .stream()
                  .filter(selection -> selection.getFilterType() == BlackoutWindowFilterType.CUSTOM)
                  .anyMatch(appSelection
                      -> appSelection.getEnvSelection().getFilterType() == EnvironmentFilterType.CUSTOM
                          && ((CustomAppFilter) appSelection).getApps().size() != 1)) {
            throw new InvalidRequestException(
                "Application filter should have exactly one app when environment filter type is CUSTOM");
          }
        });
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
      log.error("User is null when trying to publish to segment. Event will be skipped. Event Type: {}, accountId={}",
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
      log.info("Deleting Deployment Freeze window(s)");
      Query<GovernanceConfig> query =
          wingsPersistence.createQuery(GovernanceConfig.class).filter(GovernanceConfigKeys.accountId, accountId);
      GovernanceConfig config = query.get();
      if (wingsPersistence.delete(query)) {
        auditServiceHelper.reportDeleteForAuditingUsingAccountId(accountId, config);
      }
    }
  }
}
