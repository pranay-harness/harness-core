package software.wings.service.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.persistence.HQuery.excludeAuthority;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.isBlank;

import com.google.common.base.Splitter;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.configuration.DeployMode;
import io.harness.exception.InvalidRequestException;
import io.harness.persistence.HPersistence;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.app.MainConfiguration;
import software.wings.beans.FeatureFlag;
import software.wings.beans.FeatureFlag.FeatureFlagKeys;
import software.wings.beans.FeatureFlag.Scope;
import software.wings.beans.FeatureName;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.FeatureFlagService;

import java.time.Clock;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Singleton
@Slf4j
public class FeatureFlagServiceImpl implements FeatureFlagService {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private Clock clock;
  @Inject private MainConfiguration mainConfiguration;

  private long lastEpoch;
  private final Map<FeatureName, FeatureFlag> cache = new HashMap<>();

  @Override
  public boolean isEnabledReloadCache(FeatureName featureName, String accountId) {
    synchronized (cache) {
      cache.clear();
    }
    return isEnabled(featureName, accountId);
  }

  @Override
  public void enableAccount(FeatureName featureName, String accountId) {
    logger.info("Enabling feature name :[{}] for account id: [{}]", featureName.name(), accountId);
    Query<FeatureFlag> query =
        wingsPersistence.createQuery(FeatureFlag.class).filter(FeatureFlagKeys.name, featureName.name());
    UpdateOperations<FeatureFlag> updateOperations = wingsPersistence.createUpdateOperations(FeatureFlag.class)
                                                         .addToSet(FeatureFlagKeys.accountIds, accountId)
                                                         .setOnInsert(FeatureFlagKeys.name, featureName.name())
                                                         .setOnInsert(FeatureFlagKeys.uuid, generateUuid())
                                                         .setOnInsert(FeatureFlagKeys.obsolete, false)
                                                         .setOnInsert(FeatureFlagKeys.enabled, false);
    FeatureFlag featureFlag =
        wingsPersistence.findAndModify(query, updateOperations, HPersistence.upsertReturnNewOptions);
    synchronized (cache) {
      cache.put(featureName, featureFlag);
    }
    logger.info("Enabled feature name :[{}] for account id: [{}]", featureName.name(), accountId);
  }

  @Override
  public FeatureFlag updateFeatureFlagForAccount(String featureName, String accountId, boolean enabled) {
    Optional<FeatureFlag> featureFlagOptional = getFeatureFlag(FeatureName.valueOf(featureName));
    FeatureFlag featureFlag =
        featureFlagOptional.orElseThrow(() -> new InvalidRequestException("Invalid feature flag name: " + featureName));
    if (Objects.isNull(featureFlag.getAccountIds())) {
      featureFlag.setAccountIds(Sets.newHashSet());
    }

    // cannot update if it is globally enabled
    if (featureFlag.isEnabled()) {
      logger.info("Feature flag is enabled globally, disable it first to enable or disable for account.");
      throw new InvalidRequestException(
          "Feature flag is enabled globally, cannot enable/disable for account " + accountId);
    }

    if (enabled) {
      featureFlag.getAccountIds().add(accountId);
    } else {
      featureFlag.getAccountIds().remove(accountId);
    }
    wingsPersistence.save(featureFlag);
    synchronized (cache) {
      cache.put(FeatureName.valueOf(featureName), featureFlag);
    }
    return featureFlag;
  }

  @Override
  public void enableGlobally(FeatureName featureName) {
    logger.info("Enabling feature name :[{}] globally", featureName.name());
    Query<FeatureFlag> query =
        wingsPersistence.createQuery(FeatureFlag.class).filter(FeatureFlagKeys.name, featureName.name());
    UpdateOperations<FeatureFlag> updateOperations = wingsPersistence.createUpdateOperations(FeatureFlag.class)
                                                         .setOnInsert(FeatureFlagKeys.name, featureName.name())
                                                         .setOnInsert(FeatureFlagKeys.uuid, generateUuid())
                                                         .setOnInsert(FeatureFlagKeys.obsolete, Boolean.FALSE)
                                                         .set(FeatureFlagKeys.enabled, Boolean.TRUE);
    FeatureFlag featureFlag =
        wingsPersistence.findAndModify(query, updateOperations, HPersistence.upsertReturnNewOptions);
    synchronized (cache) {
      cache.put(featureName, featureFlag);
    }
    logger.info("Enabled feature name :[{}] globally", featureName.name());
  }

  @Override
  public boolean isGlobalEnabled(FeatureName featureName) {
    if (featureName.getScope() != Scope.GLOBAL) {
      logger.warn("FeatureFlag {} is not global", featureName.name(), new Exception(""));
    }
    return isEnabled(featureName, null);
  }

  @Override
  public Optional<FeatureFlag> getFeatureFlag(@NonNull FeatureName featureName) {
    FeatureFlag featureFlag;
    synchronized (cache) {
      // if the last access to cache was in different epoch reset it. This will allow for potentially outdated
      // objects to be replaced, and the potential change will be in a relatively same time on all managers.
      long epoch = clock.millis() / Duration.ofMinutes(5).toMillis();
      if (lastEpoch != epoch) {
        lastEpoch = epoch;
        cache.clear();
      }

      featureFlag = cache.computeIfAbsent(featureName,
          key
          -> wingsPersistence.createQuery(FeatureFlag.class, excludeAuthority)
                 .filter(FeatureFlagKeys.name, key.name())
                 .get());
    }
    return Optional.ofNullable(featureFlag);
  }

  @Override
  public boolean isEnabled(@NonNull FeatureName featureName, String accountId) {
    Optional<FeatureFlag> featureFlagOptional = getFeatureFlag(featureName);

    if (featureFlagOptional.isPresent()) {
      FeatureFlag featureFlag = featureFlagOptional.get();

      if (featureFlag.isEnabled()) {
        return true;
      }

      if (isEmpty(accountId) && featureName.getScope() == Scope.PER_ACCOUNT) {
        logger.error("FeatureFlag isEnabled check without accountId", new Exception(""));
        return false;
      }

      if (isNotEmpty(featureFlag.getAccountIds())) {
        if (featureName.getScope() == Scope.GLOBAL) {
          logger.error("A global FeatureFlag isEnabled per specific accounts", new Exception(""));
          return false;
        }
        return featureFlag.getAccountIds().contains(accountId);
      }
    }

    return false;
  }

  @Override
  public Set<String> getAccountIds(@NonNull FeatureName featureName) {
    FeatureFlag featureFlag = getFeatureFlag(featureName).orElse(null);
    if (featureFlag == null || isEmpty(featureFlag.getAccountIds())) {
      return new HashSet<>();
    }
    if (featureName.getScope() == Scope.GLOBAL) {
      logger.warn("FeatureFlag {} is global, should not have accountIds", featureName.name(), new Exception(""));
    }
    return featureFlag.getAccountIds();
  }

  @Override
  public void initializeFeatureFlags() {
    Set<String> definedNames = Arrays.stream(FeatureName.values()).map(FeatureName::name).collect(toSet());

    // Mark persisted flags that are no longer defined as obsolete
    wingsPersistence.update(wingsPersistence.createQuery(FeatureFlag.class, excludeAuthority)
                                .filter(FeatureFlagKeys.obsolete, false)
                                .field(FeatureFlagKeys.name)
                                .notIn(definedNames),
        wingsPersistence.createUpdateOperations(FeatureFlag.class).set(FeatureFlagKeys.obsolete, true));

    // Mark persisted flags that are defined as not obsolete
    wingsPersistence.update(wingsPersistence.createQuery(FeatureFlag.class, excludeAuthority)
                                .filter(FeatureFlagKeys.obsolete, true)
                                .field(FeatureFlagKeys.name)
                                .in(definedNames),
        wingsPersistence.createUpdateOperations(FeatureFlag.class).set(FeatureFlagKeys.obsolete, false));

    // Delete flags that were marked obsolete more than ten days ago
    wingsPersistence.delete(wingsPersistence.createQuery(FeatureFlag.class, excludeAuthority)
                                .filter(FeatureFlagKeys.obsolete, true)
                                .field(FeatureFlagKeys.lastUpdatedAt)
                                .lessThan(clock.millis() - TimeUnit.DAYS.toMillis(10)));

    // Persist new flags initialized as enabled false
    Set<String> persistedNames = wingsPersistence.createQuery(FeatureFlag.class, excludeAuthority)
                                     .project(FeatureFlagKeys.name, true)
                                     .asList()
                                     .stream()
                                     .map(FeatureFlag::getName)
                                     .collect(toSet());
    List<FeatureFlag> newFeatureFlags = definedNames.stream()
                                            .filter(name -> !persistedNames.contains(name))
                                            .map(name -> FeatureFlag.builder().name(name).enabled(false).build())
                                            .collect(toList());
    wingsPersistence.save(newFeatureFlags);

    // For on-prem, set all enabled values from the list of enabled flags in the configuration
    if (DeployMode.isOnPrem(mainConfiguration.getDeployMode().name())) {
      String features = mainConfiguration.getFeatureNames();
      List<String> enabled =
          isBlank(features) ? emptyList() : Splitter.on(',').omitEmptyStrings().trimResults().splitToList(features);
      for (String name : definedNames) {
        wingsPersistence.update(
            wingsPersistence.createQuery(FeatureFlag.class, excludeAuthority).filter(FeatureFlagKeys.name, name),
            wingsPersistence.createUpdateOperations(FeatureFlag.class)
                .set(FeatureFlagKeys.enabled, enabled.contains(name)));
      }
    }
  }

  /**
   * Used to return list of feature flags to admin tool
   * @return List of all feature flags defined
   */
  @Override
  public List<FeatureFlag> getAllFeatureFlags() {
    return wingsPersistence.createQuery(FeatureFlag.class).asList();
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
    wingsPersistence.save(featureFlag);
    synchronized (cache) {
      cache.put(FeatureName.valueOf(featureFlagName), featureFlag);
    }
    return Optional.of(featureFlag);
  }
}
