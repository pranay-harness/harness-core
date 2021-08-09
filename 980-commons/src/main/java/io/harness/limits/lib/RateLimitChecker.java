package io.harness.limits.lib;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.limits.impl.model.RateLimit;

@OwnedBy(PL)
public interface RateLimitChecker extends LimitChecker {
  RateLimit getLimit();
}
