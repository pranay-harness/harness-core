package io.harness.event.timeseries.processor.utils;

import io.harness.beans.FeatureFlag;
import io.harness.beans.FeatureName;
import io.harness.configuration.DeployMode;
import io.harness.exception.InvalidRequestException;
import io.harness.ff.FeatureFlagService;

import software.wings.app.MainConfiguration;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Optional;

@Singleton
public final class FFUtils {
  @Inject private FeatureFlagService featureFlagService;
  @Inject private MainConfiguration mainConfiguration;

  public void updateFeatureFlagForAccount(FeatureName featureName, String accountId, boolean status) {
    if (mainConfiguration.getDeployMode() != null && DeployMode.isOnPrem(mainConfiguration.getDeployMode().name())) {
      Optional<FeatureFlag> featureFlagOptional = featureFlagService.getFeatureFlag(featureName);
      FeatureFlag featureFlag = featureFlagOptional.orElseThrow(
          () -> new InvalidRequestException("Invalid feature flag name: " + featureName.toString()));
      // set global ENABLED field for On-Prem version
      featureFlag.setEnabled(status);
      featureFlagService.updateFeatureFlag(featureFlag.getName(), featureFlag);
    } else {
      // SAAS handling
      featureFlagService.updateFeatureFlagForAccount(featureName.toString(), accountId, status);
    }
  }
}
