package software.wings.features.api;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

@TargetModule(HarnessModule._940_FEATURE_FLAG)
@OwnedBy(PL)
public interface UsageLimitedFeature extends RestrictedFeature {
  int getMaxUsageAllowedForAccount(String accountId);

  int getMaxUsageAllowed(String accountType);

  int getUsage(String accountId);
}
