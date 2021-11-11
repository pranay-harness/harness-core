package io.harness.utils.featureflaghelper;

import io.harness.account.AccountClient;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.FeatureName;
import io.harness.remote.client.RestClientUtils;

import com.google.inject.Inject;

@OwnedBy(HarnessTeam.PL)
public class NGFeatureFlagHelperServiceImpl implements FeatureFlagHelperService {
  @Inject AccountClient accountClient;

  @Override
  public boolean isEnabled(String accountId, FeatureName featureName) {
    return RestClientUtils.getResponse(accountClient.isFeatureFlagEnabled(featureName.name(), accountId));
  }
}
