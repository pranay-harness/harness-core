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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;

public class FeatureFlagServiceImpl implements FeatureFlagService {
  private final Logger logger = LoggerFactory.getLogger(getClass());

  @Inject private WingsPersistence wingsPersistence;

  @Override
  public boolean isEnabled(FeatureName name, String accountId) {
    if (name == null) {
      // we don't want to throw an exception - we just want to log the error
      logger.error("FeatureFlag name is null or missing!");
      return false;
    }

    FeatureFlag featureFlag = wingsPersistence.createQuery(FeatureFlag.class).field("name").equal(name).get();

    if (featureFlag == null) {
      // we don't want to throw an exception - we just want to log the error
      logger.error("FeatureFlag " + name.name() + " not found.");
      return false;
    }

    if (featureFlag.isEnabled()) {
      return true;
    }

    if (isEmpty(accountId)) {
      // we don't want to throw an exception - we just want to log the error
      logger.error("FeatureFlag accountId is null or missing!");
      return false;
    }

    if (isNotEmpty(featureFlag.getWhiteListedAccountIds())) {
      if (featureFlag.getWhiteListedAccountIds().contains(accountId)) {
        return true;
      }
    }

    return false;
  }

  @Override
  public void initializeMissingFlags() {
    Set<FeatureName> existing = wingsPersistence.createQuery(FeatureFlag.class)
                                    .asList()
                                    .stream()
                                    .map(FeatureFlag::getName)
                                    .collect(Collectors.toSet());
    List<FeatureFlag> newFeatures = Arrays.stream(FeatureName.values())
                                        .filter(featureName -> !existing.contains(featureName))
                                        .map(featureName
                                            -> FeatureFlag.builder()
                                                   .name(featureName)
                                                   .enabled(false)
                                                   .whiteListedAccountIds(new HashSet<>())
                                                   .build())
                                        .collect(Collectors.toList());
    wingsPersistence.save(newFeatures);
  }
}
