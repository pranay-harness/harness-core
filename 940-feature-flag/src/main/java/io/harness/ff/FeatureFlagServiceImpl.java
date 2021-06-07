package io.harness.ff;

import static io.harness.beans.FeatureName.NEXT_GEN_ENABLED;
import static io.harness.beans.FeatureName.NG_ACCESS_CONTROL_MIGRATION;
import static io.harness.beans.FeatureName.NG_RBAC_ENABLED;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.persistence.HQuery.excludeAuthority;

import static java.lang.System.currentTimeMillis;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.isBlank;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.FeatureFlag;
import io.harness.beans.FeatureFlag.FeatureFlagKeys;
import io.harness.beans.FeatureFlag.Scope;
import io.harness.beans.FeatureName;
import io.harness.cf.CfMigrationConfig;
import io.harness.cf.client.api.CfClient;
import io.harness.cf.client.dto.Target;
import io.harness.configuration.DeployMode;
import io.harness.eventsframework.EventsFrameworkConstants;
import io.harness.eventsframework.api.Producer;
import io.harness.eventsframework.featureflag.FeatureFlagChangeDTO;
import io.harness.eventsframework.producer.Message;
import io.harness.exception.InvalidRequestException;
import io.harness.persistence.HPersistence;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

@Singleton
@Slf4j
@OwnedBy(HarnessTeam.PL)
public class FeatureFlagServiceImpl implements FeatureFlagService {
  @Inject private HPersistence persistence;
  @Inject(optional = true)
  @Nullable
  @Named(EventsFrameworkConstants.FEATURE_FLAG_STREAM)
  private Producer eventProducer;

  private final List<FeatureName> featureFlagsToSendEvent =
      Lists.newArrayList(ImmutableList.of(NG_ACCESS_CONTROL_MIGRATION, NG_RBAC_ENABLED, NEXT_GEN_ENABLED));
  private long lastEpoch;
  private final Map<FeatureName, FeatureFlag> cache = new HashMap<>();
  @Inject CfMigrationService cfMigrationService;
  @Inject CfMigrationConfig cfMigrationConfig;
  @Inject CfClient cfClient;
  @Inject FeatureFlagConfig featureFlagConfig;
  @Override
  public boolean isEnabledReloadCache(FeatureName featureName, String accountId) {
    synchronized (cache) {
      cache.clear();
    }
    return isEnabled(featureName, accountId);
  }

  @Override
  public void enableAccount(FeatureName featureName, String accountId) {
    log.info("Enabling feature name :[{}] for account id: [{}]", featureName.name(), accountId);
    Query<FeatureFlag> query =
        persistence.createQuery(FeatureFlag.class).filter(FeatureFlagKeys.name, featureName.name());
    UpdateOperations<FeatureFlag> updateOperations = persistence.createUpdateOperations(FeatureFlag.class)
                                                         .addToSet(FeatureFlagKeys.accountIds, accountId)
                                                         .setOnInsert(FeatureFlagKeys.name, featureName.name())
                                                         .setOnInsert(FeatureFlagKeys.uuid, generateUuid())
                                                         .setOnInsert(FeatureFlagKeys.obsolete, false)
                                                         .setOnInsert(FeatureFlagKeys.enabled, false);
    FeatureFlag featureFlag = persistence.findAndModify(query, updateOperations, HPersistence.upsertReturnNewOptions);
    if (featureFlagsToSendEvent.contains(featureName)) {
      publishEvent(accountId, featureName, true);
    }
    synchronized (cache) {
      cache.put(featureName, featureFlag);
    }

    cfMigrationService.syncFeatureFlagWithCF(featureFlag);

    log.info("Enabled feature name :[{}] for account id: [{}]", featureName.name(), accountId);
  }

  private void publishEvent(String accountId, FeatureName featureName, boolean enable) {
    try {
      if (eventProducer != null) {
        eventProducer.send(Message.newBuilder()
                               .putAllMetadata(ImmutableMap.of("accountId", accountId))
                               .setData(FeatureFlagChangeDTO.newBuilder()
                                            .setAccountId(accountId)
                                            .setEnable(enable)
                                            .setFeatureName(featureName.toString())
                                            .build()
                                            .toByteString())
                               .build());
      }
    } catch (Exception ex) {
      log.error("Failed to publish account change event for enabling next gen via event framework.", ex);
    }
  }

  @Override
  public FeatureFlag updateFeatureFlagForAccount(String featureName, String accountId, boolean enabled) {
    Optional<FeatureFlag> featureFlagOptional = getFeatureFlag(FeatureName.valueOf(featureName));
    FeatureFlag featureFlag =
        featureFlagOptional.orElseThrow(() -> new InvalidRequestException("Invalid feature flag name: " + featureName));
    if (Objects.isNull(featureFlag.getAccountIds())) {
      featureFlag.setAccountIds(Sets.newHashSet());
    }

    if (enabled) {
      featureFlag.getAccountIds().add(accountId);
    } else {
      featureFlag.getAccountIds().remove(accountId);
    }
    persistence.save(featureFlag);
    if (featureFlagsToSendEvent.contains(FeatureName.valueOf(featureName))) {
      publishEvent(accountId, FeatureName.valueOf(featureName), enabled);
    }
    synchronized (cache) {
      cache.put(FeatureName.valueOf(featureName), featureFlag);
    }
    return featureFlag;
  }

  @Override
  public void enableGlobally(FeatureName featureName) {
    log.info("Enabling feature name :[{}] globally", featureName.name());
    Query<FeatureFlag> query =
        persistence.createQuery(FeatureFlag.class).filter(FeatureFlagKeys.name, featureName.name());
    UpdateOperations<FeatureFlag> updateOperations = persistence.createUpdateOperations(FeatureFlag.class)
                                                         .setOnInsert(FeatureFlagKeys.name, featureName.name())
                                                         .setOnInsert(FeatureFlagKeys.uuid, generateUuid())
                                                         .setOnInsert(FeatureFlagKeys.obsolete, Boolean.FALSE)
                                                         .set(FeatureFlagKeys.enabled, Boolean.TRUE);
    FeatureFlag featureFlag = persistence.findAndModify(query, updateOperations, HPersistence.upsertReturnNewOptions);
    synchronized (cache) {
      cache.put(featureName, featureFlag);
    }
    cfMigrationService.syncFeatureFlagWithCF(featureFlag);

    log.info("Enabled feature name :[{}] globally", featureName.name());
  }

  @Override
  public List<FeatureFlag> getGloballyEnabledFeatureFlags() {
    List<FeatureFlag> globallyEnabledFeatureFlag = new ArrayList<>();

    getAllFeatureFlags().forEach(featureFlag -> {
      if (featureFlag.isEnabled()) {
        globallyEnabledFeatureFlag.add(featureFlag);
      }
    });

    return globallyEnabledFeatureFlag;
  }
  @Override
  public boolean isGlobalEnabled(FeatureName featureName) {
    switch (featureFlagConfig.getFeatureFlagSystem()) {
      case CF:
        return cfFeatureFlagEvaluation(featureName, null);
      case LOCAL:
      default:
        return isLocalGlobalEnabled(featureName);
    }
  }

  private boolean isLocalGlobalEnabled(FeatureName featureName) {
    if (featureName.getScope() != Scope.GLOBAL) {
      log.warn("FeatureFlag {} is not global", featureName.name(), new Exception(""));
    }
    return isEnabled(featureName, null);
  }

  @Override
  public boolean isNotGlobalEnabled(FeatureName featureName) {
    return !isGlobalEnabled(featureName);
  }

  @Override
  public Optional<FeatureFlag> getFeatureFlag(@NonNull FeatureName featureName) {
    FeatureFlag featureFlag;
    synchronized (cache) {
      // if the last access to cache was in different epoch reset it. This will allow for potentially outdated
      // objects to be replaced, and the potential change will be in a relatively same time on all managers.
      long epoch = currentTimeMillis() / Duration.ofMinutes(5).toMillis();
      if (lastEpoch != epoch) {
        lastEpoch = epoch;
        cache.clear();
      }

      featureFlag = cache.computeIfAbsent(featureName,
          key
          -> persistence.createQuery(FeatureFlag.class, excludeAuthority)
                 .filter(FeatureFlagKeys.name, key.name())
                 .get());
    }
    return Optional.ofNullable(featureFlag);
  }

  @Override
  public boolean isEnabled(@NonNull FeatureName featureName, String accountId) {
    switch (featureFlagConfig.getFeatureFlagSystem()) {
      case CF:
        return cfFeatureFlagEvaluation(featureName, accountId);
      case LOCAL:
      default:
        return localFeatureFlagEvaluation(featureName, accountId);
    }
  }

  private boolean cfFeatureFlagEvaluation(@NonNull FeatureName featureName, String accountId) {
    if (Scope.GLOBAL.equals(featureName.getScope()) || isEmpty(accountId)) {
      /**
       * If accountID is null or empty, use a static accountID
       */
      accountId = FeatureFlagConstants.STATIC_ACCOUNT_ID;
    }
    Target target = Target.builder().identifier(accountId).name(accountId).build();
    return cfClient.boolVariation(featureName.name(), target, false);
  }

  private boolean localFeatureFlagEvaluation(@NonNull FeatureName featureName, String accountId) {
    Optional<FeatureFlag> featureFlagOptional = getFeatureFlag(featureName);
    boolean featureValue = false;

    if (featureFlagOptional.isPresent()) {
      try {
        FeatureFlag featureFlag = featureFlagOptional.get();
        if (featureFlag.isEnabled()) {
          featureValue = true;
          return featureValue;
        }

        if (isEmpty(accountId) && featureName.getScope() == Scope.PER_ACCOUNT) {
          log.debug("FeatureFlag isEnabled check without accountId", new Exception(""));
          featureValue = false;
          return featureValue;
        }

        if (isNotEmpty(featureFlag.getAccountIds())) {
          if (featureName.getScope() == Scope.GLOBAL) {
            log.debug("A global FeatureFlag isEnabled per specific accounts", new Exception(""));
            featureValue = false;
            return featureValue;
          }
          featureValue = featureFlag.getAccountIds().contains(accountId);
          return featureValue;
        }
      } finally {
        cfMigrationService.verifyBehaviorWithCF(featureName, featureValue, accountId);
      }
    }

    return featureValue;
  }

  @Override
  public boolean isEnabledForAllAccounts(FeatureName featureName) {
    switch (featureFlagConfig.getFeatureFlagSystem()) {
      case CF:
        return cfFeatureFlagEvaluation(featureName, null);
      case LOCAL:
      default:
        return localIsEnabledForAllAccounts(featureName);
    }
  }

  private boolean localIsEnabledForAllAccounts(FeatureName featureName) {
    Optional<FeatureFlag> featureFlagOptional = getFeatureFlag(featureName);
    if (featureFlagOptional.isPresent()) {
      FeatureFlag featureFlag = featureFlagOptional.get();

      if (featureFlag.isEnabled()) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean isNotEnabled(FeatureName featureName, String accountId) {
    return !isEnabled(featureName, accountId);
  }

  @Override
  public Set<String> getAccountIds(@NonNull FeatureName featureName) {
    FeatureFlag featureFlag = getFeatureFlag(featureName).orElse(null);
    if (featureFlag == null || isEmpty(featureFlag.getAccountIds())) {
      return new HashSet<>();
    }
    if (featureName.getScope() == Scope.GLOBAL) {
      log.warn("FeatureFlag {} is global, should not have accountIds", featureName.name(), new Exception(""));
    }
    return featureFlag.getAccountIds();
  }

  @Override
  public void initializeFeatureFlags(DeployMode deployMode, String featureNames) {
    Set<String> definedNames = Arrays.stream(FeatureName.values()).map(FeatureName::name).collect(toSet());

    // Mark persisted flags that are no longer defined as obsolete
    persistence.update(persistence.createQuery(FeatureFlag.class, excludeAuthority)
                           .filter(FeatureFlagKeys.obsolete, false)
                           .field(FeatureFlagKeys.name)
                           .notIn(definedNames),
        persistence.createUpdateOperations(FeatureFlag.class).set(FeatureFlagKeys.obsolete, true));

    // Mark persisted flags that are defined as not obsolete
    persistence.update(persistence.createQuery(FeatureFlag.class, excludeAuthority)
                           .filter(FeatureFlagKeys.obsolete, true)
                           .field(FeatureFlagKeys.name)
                           .in(definedNames),
        persistence.createUpdateOperations(FeatureFlag.class).set(FeatureFlagKeys.obsolete, false));

    // Delete flags that were marked obsolete more than ten days ago
    persistence.delete(persistence.createQuery(FeatureFlag.class, excludeAuthority)
                           .filter(FeatureFlagKeys.obsolete, true)
                           .field(FeatureFlagKeys.lastUpdatedAt)
                           .lessThan(currentTimeMillis() - TimeUnit.DAYS.toMillis(10)));

    // Persist new flags initialized as enabled false
    Set<String> persistedNames = persistence.createQuery(FeatureFlag.class, excludeAuthority)
                                     .project(FeatureFlagKeys.name, true)
                                     .asList()
                                     .stream()
                                     .map(FeatureFlag::getName)
                                     .collect(toSet());
    List<FeatureFlag> newFeatureFlags = definedNames.stream()
                                            .filter(name -> !persistedNames.contains(name))
                                            .map(name -> FeatureFlag.builder().name(name).enabled(false).build())
                                            .collect(toList());
    persistence.save(newFeatureFlags);

    // For on-prem, set all enabled values from the list of enabled flags in the configuration
    if (DeployMode.isOnPrem(deployMode.name())) {
      List<String> enabled = isBlank(featureNames)
          ? emptyList()
          : Splitter.on(',').omitEmptyStrings().trimResults().splitToList(featureNames);
      for (String name : definedNames) {
        persistence.update(
            persistence.createQuery(FeatureFlag.class, excludeAuthority).filter(FeatureFlagKeys.name, name),
            persistence.createUpdateOperations(FeatureFlag.class).set(FeatureFlagKeys.enabled, enabled.contains(name)));
      }
    }
    /**
     *
     * If featureFlagConfig mode is CF and sync is enabled, then only sync creation of feature flags to CF
     *
     * else
     *
     * if Migration is enabled,
     * migrate everything including current configuration over to CF -> This will be required initially as we migrate
     * our current system over to CF
     *
     */
    if (FeatureFlagSystem.CF.equals(featureFlagConfig.getFeatureFlagSystem())
        && featureFlagConfig.isSyncFeaturesToCF()) {
      log.info("FeatureFlagSystem is CF and sync is turned on, will only create flags not in the system");
      cfMigrationService.syncAllFlagsWithCFServer(this, false);
    } else if (cfMigrationConfig.isEnabled()) {
      log.info("migration is enabled, will sync feature flags and rules");
      cfMigrationService.syncAllFlagsWithCFServer(this, true);
      return;
    }
  }

  /**
   * Used to return list of feature flags to admin tool
   * @return List of all feature flags defined
   */
  @Override
  public List<FeatureFlag> getAllFeatureFlags() {
    return persistence.createQuery(FeatureFlag.class).asList();
  }

  /**
   * used by admin tool to batch add/remove accounts in feature flag and enable/disable feature flag globally
   * @param featureFlagName name
   * @param featureFlag feature flag
   * @return updated feature flag
   */
  @Override
  public Optional<FeatureFlag> updateFeatureFlag(String featureFlagName, FeatureFlag featureFlag) {
    Optional<FeatureFlag> featureFlagOptional = getFeatureFlag(FeatureName.valueOf(featureFlagName));
    if (!featureFlagOptional.isPresent()) {
      return Optional.empty();
    }
    persistence.save(featureFlag);
    if (featureFlagsToSendEvent.contains(FeatureName.valueOf(featureFlagName))) {
      FeatureFlag existingFeatureFlag = featureFlagOptional.get();
      Set<String> existingAccounts =
          existingFeatureFlag.getAccountIds() != null ? existingFeatureFlag.getAccountIds() : emptySet();
      Set<String> newAccounts = featureFlag.getAccountIds() != null ? featureFlag.getAccountIds() : emptySet();
      Set<String> accountsAdded = Sets.difference(newAccounts, existingAccounts);
      Set<String> accountsRemoved = Sets.difference(existingAccounts, newAccounts);

      // for accounts which have been added, send enabled event
      accountsAdded.forEach(account -> publishEvent(account, FeatureName.valueOf(featureFlagName), true));

      // for accounts which have been removed, send disabled event
      accountsRemoved.forEach(account -> publishEvent(account, FeatureName.valueOf(featureFlagName), false));
    }
    synchronized (cache) {
      cache.put(FeatureName.valueOf(featureFlagName), featureFlag);
    }

    cfMigrationService.syncFeatureFlagWithCF(featureFlag);
    return Optional.of(featureFlag);
  }

  /**
   * Removes an account from the FeatureFlags collection
   * @param accountId
   */
  @Override
  public void removeAccountReferenceFromAllFeatureFlags(String accountId) {
    List<FeatureFlag> featureFlags = getAllFeatureFlags();
    for (FeatureFlag featureFlag : featureFlags) {
      try {
        updateFeatureFlagForAccount(featureFlag.getName(), accountId, false);
      } catch (Exception e) {
        log.error(
            "Exception occurred while deleting account {} from FeatureFlag {}", accountId, featureFlag.getName(), e);
      }
    }
  }
}
