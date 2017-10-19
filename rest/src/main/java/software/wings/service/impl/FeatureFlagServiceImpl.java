package software.wings.service.impl;

import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.FeatureFlag;
import software.wings.beans.FeatureFlag.FeatureName;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.FeatureFlagService;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;

public class FeatureFlagServiceImpl implements FeatureFlagService {
  private final Logger logger = LoggerFactory.getLogger(getClass());

  @Inject private WingsPersistence wingsPersistence;

  @Override
  public boolean isEnabled(@NotNull FeatureName featureName, String accountId) {
    FeatureFlag featureFlag =
        wingsPersistence.createQuery(FeatureFlag.class).field("name").equal(featureName.name()).get();

    if (featureFlag == null) {
      // we don't want to throw an exception - we just want to log the error
      logger.error("FeatureFlag " + featureName.name() + " not found.");
      return false;
    }

    if (featureFlag.isEnabled()) {
      return true;
    }

    if (isEmpty(accountId)) {
      // we don't want to throw an exception - we just want to log the error
      logger.error("FeatureFlag isEnabled check without accountId");
      return false;
    }

    if (isNotEmpty(featureFlag.getAccountIds())) {
      if (featureFlag.getAccountIds().contains(accountId)) {
        return true;
      }
    }

    return false;
  }

  @Override
  public void initializeFeatureFlags() {
    List<FeatureFlag> persistedFeatureFlags = wingsPersistence.createQuery(FeatureFlag.class).asList();
    Set<String> definedNames = Arrays.stream(FeatureName.values()).map(FeatureName::name).collect(Collectors.toSet());
    persistedFeatureFlags.forEach(flag -> flag.setObsolete(!definedNames.contains(flag.getName())));
    wingsPersistence.save(persistedFeatureFlags);
    Set<String> persistedNames = persistedFeatureFlags.stream().map(FeatureFlag::getName).collect(Collectors.toSet());
    List<FeatureFlag> newFeatureFlags = definedNames.stream()
                                            .filter(name -> !persistedNames.contains(name))
                                            .map(name -> FeatureFlag.builder().name(name).enabled(false).build())
                                            .collect(Collectors.toList());
    wingsPersistence.save(newFeatureFlags);
  }
}
