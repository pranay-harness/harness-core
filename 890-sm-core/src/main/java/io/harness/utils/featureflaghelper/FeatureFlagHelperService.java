package io.harness.utils.featureflaghelper;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.FeatureName;

import javax.validation.constraints.NotNull;

@OwnedBy(HarnessTeam.PL)
public interface FeatureFlagHelperService {
  boolean isEnabled(String accountId, @NotNull FeatureName featureName);
}
