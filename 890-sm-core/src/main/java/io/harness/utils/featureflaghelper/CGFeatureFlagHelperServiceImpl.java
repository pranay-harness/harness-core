package io.harness.utils.featureflaghelper;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.FeatureName;
import io.harness.ff.FeatureFlagService;

import com.google.inject.Inject;

@OwnedBy(HarnessTeam.PL)
public class CGFeatureFlagHelperServiceImpl implements FeatureFlagHelperService {
  @Inject FeatureFlagService featureFlagService;

  @Override
  public boolean isEnabled(String accountId, FeatureName featureName) {
    return featureFlagService.isEnabled(featureName, accountId);
  }
}
