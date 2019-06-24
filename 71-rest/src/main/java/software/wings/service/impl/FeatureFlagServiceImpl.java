package software.wings.service.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.persistence.HQuery.excludeAuthority;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.isBlank;

import com.google.common.base.Splitter;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import lombok.extern.slf4j.Slf4j;
import software.wings.app.DeployMode;
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.validation.constraints.NotNull;

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
    cache.clear();
    return isEnabled(featureName, accountId);
  }

  @Override
  public boolean isGlobalEnabled(FeatureName featureName) {
    if (featureName.getScope() != Scope.GLOBAL) {
      logger.error(format("FeatureFlag %s is not global", featureName.name()), new Exception(""));
    }
    return isEnabled(featureName, null);
  }

  @Override
  public boolean isEnabled(@NotNull FeatureName featureName, String accountId) {
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

    if (featureFlag == null) {
      // this is common in test scenarios, lets not do anything here and just return false
      return false;
    }

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

    return false;
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
}
