package io.harness.ff;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.FeatureFlag;
import io.harness.beans.FeatureName;
import io.harness.configuration.DeployMode;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@OwnedBy(HarnessTeam.PL)
public interface FeatureFlagService {
  boolean isGlobalEnabled(FeatureName featureName);
  boolean isNotGlobalEnabled(FeatureName featureName);
  boolean isEnabled(FeatureName featureName, String accountId);
  boolean isEnabledForAllAccounts(FeatureName featureName);

  boolean isNotEnabled(FeatureName featureName, String accountId);

  void initializeFeatureFlags(DeployMode deployMode, String featureNames);

  List<FeatureFlag> getAllFeatureFlags();

  List<FeatureFlag> getGloballyEnabledFeatureFlags();

  boolean isEnabledReloadCache(FeatureName featureName, String accountId);

  void enableAccount(FeatureName featureName, String accountId);

  FeatureFlag updateFeatureFlagForAccount(String featureName, String accountId, boolean enabled);

  Optional<FeatureFlag> getFeatureFlag(FeatureName featureName);

  void enableGlobally(FeatureName featureName);

  Set<String> getAccountIds(FeatureName featureName);

  Optional<FeatureFlag> updateFeatureFlag(String featureFlagName, FeatureFlag featureFlag);
}
