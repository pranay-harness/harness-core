package io.harness.pms.helpers;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.account.AccountClient;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.FeatureName;
import io.harness.remote.client.RestClientUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PIPELINE)
@Slf4j
@Singleton
public class PmsFeatureFlagHelper {
  @Inject private AccountClient accountClient;

  public boolean isEnabled(String accountId, @NotNull FeatureName featureName) {
    return RestClientUtils.getResponse(accountClient.isFeatureFlagEnabled(featureName.name(), accountId));
  }
}
