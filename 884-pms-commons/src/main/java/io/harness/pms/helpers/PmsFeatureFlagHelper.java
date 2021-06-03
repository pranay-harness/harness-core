package io.harness.pms.helpers;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.account.AccountClient;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.FeatureFlag;
import io.harness.beans.FeatureName;
import io.harness.remote.client.RestClientUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import java.util.stream.Collectors;
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

  public List<String> listAllEnabledFeatureFlagsForAccount(String accountId) {
    return RestClientUtils.getResponse(accountClient.listAllFeatureFlagsForAccount(accountId))
        .stream()
        .filter(FeatureFlag::isEnabled)
        .map(FeatureFlag::getName)
        .collect(Collectors.toList());
  }
}
