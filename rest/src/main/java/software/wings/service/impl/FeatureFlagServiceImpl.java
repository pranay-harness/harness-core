package software.wings.service.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static software.wings.app.DeployMode.ONPREM;
import static software.wings.dl.HQuery.excludeAuthority;

import com.google.api.client.repackaged.com.google.common.base.Splitter;
import com.google.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.app.MainConfiguration;
import software.wings.beans.FeatureFlag;
import software.wings.beans.FeatureName;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.FeatureFlagService;

import java.time.Clock;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.validation.constraints.NotNull;

public class FeatureFlagServiceImpl implements FeatureFlagService {
  private static final Logger logger = LoggerFactory.getLogger(FeatureFlagServiceImpl.class);

  @Inject private WingsPersistence wingsPersistence;
  @Inject private Clock clock;
  @Inject private MainConfiguration mainConfiguration;

  @Override
  public boolean isEnabled(@NotNull FeatureName featureName, String accountId) {
    FeatureFlag featureFlag =
        wingsPersistence.createQuery(FeatureFlag.class, excludeAuthority).filter("name", featureName.name()).get();

    if (featureFlag == null) {
      // we don't want to throw an exception - we just want to log the error
      logger.info("FeatureFlag {} not found.", featureName.name());
      return false;
    }

    if (featureFlag.isEnabled()) {
      return true;
    }

    if (isEmpty(accountId)) {
      // we don't want to throw an exception - we just want to log the error
      logger.warn("FeatureFlag isEnabled check without accountId", new Exception(""));
      return false;
    }

    return isNotEmpty(featureFlag.getAccountIds()) && featureFlag.getAccountIds().contains(accountId);
  }

  @Override
  public void initializeFeatureFlags() {
    Set<String> definedNames = Arrays.stream(FeatureName.values()).map(FeatureName::name).collect(toSet());

    // Mark persisted flags that are no longer defined as obsolete
    wingsPersistence.update(wingsPersistence.createQuery(FeatureFlag.class, excludeAuthority)
                                .filter("obsolete", false)
                                .field("name")
                                .notIn(definedNames),
        wingsPersistence.createUpdateOperations(FeatureFlag.class).set("obsolete", true));

    // Delete flags that were marked obsolete more than ten days ago
    wingsPersistence.delete(wingsPersistence.createQuery(FeatureFlag.class, excludeAuthority)
                                .filter("obsolete", true)
                                .field("lastUpdatedAt")
                                .lessThan(clock.millis() - TimeUnit.DAYS.toMillis(10)));

    // Persist new flags initialized as enabled false
    Set<String> persistedNames = wingsPersistence.createQuery(FeatureFlag.class, excludeAuthority)
                                     .project("name", true)
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
    if (ONPREM.equals(mainConfiguration.getDeployMode())) {
      String features = mainConfiguration.getFeatureNames();
      List<String> enabled =
          isBlank(features) ? emptyList() : Splitter.on(',').omitEmptyStrings().trimResults().splitToList(features);
      for (String name : definedNames) {
        wingsPersistence.update(wingsPersistence.createQuery(FeatureFlag.class, excludeAuthority).filter("name", name),
            wingsPersistence.createUpdateOperations(FeatureFlag.class).set("enabled", enabled.contains(name)));
      }
    }
  }
}
