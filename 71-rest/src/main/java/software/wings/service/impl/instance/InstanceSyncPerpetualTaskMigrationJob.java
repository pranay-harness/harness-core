package software.wings.service.impl.instance;

import io.harness.lock.AcquiredLock;
import io.harness.lock.PersistentLocker;

import software.wings.beans.Account;
import software.wings.beans.FeatureName;
import software.wings.beans.InfrastructureMapping;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.InfrastructureMappingService;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.dropwizard.lifecycle.Managed;
import java.time.Duration;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class InstanceSyncPerpetualTaskMigrationJob implements Managed {
  private static final String LOCK_NAME = "InstanceSyncPerpetualTaskMigrationJobLock";
  private static final long DELAY_IN_MINUTES = TimeUnit.HOURS.toMinutes(6);

  @Inject private InstanceHandlerFactory instanceHandlerFactory;
  @Inject private AccountService accountService;
  @Inject private FeatureFlagService featureFlagService;
  @Inject private PersistentLocker persistentLocker;
  @Inject private InstanceSyncPerpetualTaskService instanceSyncPerpetualTaskService;
  @Inject private AppService appService;
  @Inject private InfrastructureMappingService infrastructureMappingService;

  private Map<FeatureName, InstanceSyncByPerpetualTaskHandler> featureFlagToInstanceHandlerMap;

  private ScheduledExecutorService executorService;

  @Override
  public void start() {
    featureFlagToInstanceHandlerMap = getEnablePerpetualTaskFeatureFlagsForInstanceSync();
    executorService = Executors.newSingleThreadScheduledExecutor(
        new ThreadFactoryBuilder().setNameFormat("instance-sync-perpetual-task-migration-job").build());

    executorService.scheduleWithFixedDelay(this::run, 30, DELAY_IN_MINUTES, TimeUnit.MINUTES);
  }

  @Override
  public void stop() throws InterruptedException {
    executorService.shutdownNow();
    executorService.awaitTermination(30, TimeUnit.SECONDS);
  }

  @VisibleForTesting
  public void run() {
    try (AcquiredLock<?> lock = persistentLocker.tryToAcquireLock(LOCK_NAME, Duration.ofMinutes(30))) {
      if (lock == null) {
        log.info("Couldn't acquire lock");
        return;
      }
      log.info("Instance sync Perpetual Task migration job started");
      for (FeatureName featureName : featureFlagToInstanceHandlerMap.keySet()) {
        handleFeatureFlag(featureName);
      }
      log.info("Instance sync Perpetual Task migration job completed");
    }
  }

  private Map<FeatureName, InstanceSyncByPerpetualTaskHandler> getEnablePerpetualTaskFeatureFlagsForInstanceSync() {
    Map<FeatureName, InstanceSyncByPerpetualTaskHandler> result = new EnumMap<>(FeatureName.class);
    for (InstanceHandler instanceHandler : instanceHandlerFactory.getAllInstanceHandlers()) {
      if (instanceHandler instanceof InstanceSyncByPerpetualTaskHandler) {
        InstanceSyncByPerpetualTaskHandler handler = (InstanceSyncByPerpetualTaskHandler) instanceHandler;
        result.put(handler.getFeatureFlagToEnablePerpetualTaskForInstanceSync(), handler);
      }
    }

    return result;
  }

  private void handleFeatureFlag(FeatureName featureFlag) {
    log.info("Processing Feature Flag: [{}]", featureFlag.name());
    Set<String> allAccounts = accountService.listAllAccountWithDefaultsWithoutLicenseInfo()
                                  .stream()
                                  .map(Account::getUuid)
                                  .collect(Collectors.toSet());

    Set<String> featureFlagEnabledAccounts;
    Set<String> featureFlagDisabledAccounts;
    if (featureFlagService.isGlobalEnabled(featureFlag)) {
      featureFlagEnabledAccounts = allAccounts;
      featureFlagDisabledAccounts = Collections.emptySet();
    } else {
      featureFlagEnabledAccounts = featureFlagService.getAccountIds(featureFlag);
      featureFlagDisabledAccounts = Sets.difference(allAccounts, featureFlagEnabledAccounts);
    }

    log.info("Enabling Feature Flag: [{}] for [{}] accounts", featureFlag.name(), featureFlagEnabledAccounts.size());

    for (String accountId : featureFlagEnabledAccounts) {
      enableFeatureFlagForAccount(featureFlag, accountId);
    }

    log.info("Enabled Feature Flag: [{}] for [{}] accounts", featureFlag.name(), featureFlagEnabledAccounts.size());

    log.info("Disabling Feature Flag: [{}] for [{}] accounts", featureFlag.name(), featureFlagDisabledAccounts.size());

    for (String accountId : featureFlagDisabledAccounts) {
      disableFeatureFlagForAccount(featureFlag, accountId);
    }

    log.info("Disabled Feature Flag: [{}] for [{}] accounts", featureFlag.name(), featureFlagDisabledAccounts.size());

    log.info("Processed Feature Flag: [{}]", featureFlag.name());
  }

  private void enableFeatureFlagForAccount(FeatureName featureFlag, String accountId) {
    List<String> appIds = appService.getAppIdsByAccountId(accountId);
    for (String appId : appIds) {
      enableFeatureFlagForInfrastructureMappings(featureFlag, infrastructureMappingService.get(appId));
    }
  }

  private void enableFeatureFlagForInfrastructureMappings(
      FeatureName featureFlag, List<InfrastructureMapping> infrastructureMappings) {
    for (InfrastructureMapping infrastructureMapping : infrastructureMappings) {
      if (isFeatureFlagApplicableToInfraMapping(featureFlag, infrastructureMapping)) {
        instanceSyncPerpetualTaskService.createPerpetualTasks(infrastructureMapping);
      }
    }
  }

  private boolean isFeatureFlagApplicableToInfraMapping(
      FeatureName featureFlag, InfrastructureMapping infrastructureMapping) {
    InstanceHandler instanceHandler = instanceHandlerFactory.getInstanceHandler(infrastructureMapping);
    return instanceHandler instanceof InstanceSyncByPerpetualTaskHandler
        && (((InstanceSyncByPerpetualTaskHandler) instanceHandler).getFeatureFlagToEnablePerpetualTaskForInstanceSync())
        == featureFlag;
  }

  private void disableFeatureFlagForAccount(FeatureName featureFlag, String accountId) {
    List<String> appIds = appService.getAppIdsByAccountId(accountId);
    for (String appId : appIds) {
      disableFeatureFlagForInfrastructureMappings(featureFlag, infrastructureMappingService.get(appId));
    }
  }

  private void disableFeatureFlagForInfrastructureMappings(
      FeatureName featureFlag, List<InfrastructureMapping> infrastructureMappings) {
    for (InfrastructureMapping infrastructureMapping : infrastructureMappings) {
      if (isFeatureFlagApplicableToInfraMapping(featureFlag, infrastructureMapping)) {
        instanceSyncPerpetualTaskService.deletePerpetualTasks(infrastructureMapping);
      }
    }
  }
}
