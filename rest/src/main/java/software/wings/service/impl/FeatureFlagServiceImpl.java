package software.wings.service.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import com.google.api.client.repackaged.com.google.common.base.Throwables;
import com.google.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.FeatureFlag;
import software.wings.beans.FeatureName;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.FeatureFlagService;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import javax.validation.constraints.NotNull;

public class FeatureFlagServiceImpl implements FeatureFlagService {
  private static final Logger logger = LoggerFactory.getLogger(FeatureFlagServiceImpl.class);

  @Inject private WingsPersistence wingsPersistence;

  @Override
  public boolean isEnabled(@NotNull FeatureName featureName, String accountId) {
    FeatureFlag featureFlag =
        wingsPersistence.createQuery(FeatureFlag.class).field("name").equal(featureName.name()).get();

    if (featureFlag == null) {
      // we don't want to throw an exception - we just want to log the error
      logger.info("FeatureFlag " + featureName.name() + " not found.");
      return false;
    }

    if (featureFlag.isEnabled()) {
      return true;
    }

    if (isEmpty(accountId)) {
      // we don't want to throw an exception - we just want to log the error
      logger.warn(
          "FeatureFlag isEnabled check without accountId\n{}", Throwables.getStackTraceAsString(new Exception("")));
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
    Set<String> definedNames = Arrays.stream(FeatureName.values()).map(FeatureName::name).collect(toSet());
    persistedFeatureFlags.forEach(flag -> flag.setObsolete(!definedNames.contains(flag.getName())));
    wingsPersistence.save(persistedFeatureFlags);
    Set<String> persistedNames = persistedFeatureFlags.stream().map(FeatureFlag::getName).collect(toSet());
    List<FeatureFlag> newFeatureFlags = definedNames.stream()
                                            .filter(name -> !persistedNames.contains(name))
                                            .map(name -> FeatureFlag.builder().name(name).enabled(false).build())
                                            .collect(toList());
    wingsPersistence.save(newFeatureFlags);
  }
}
