package io.harness.limits.lib;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(PL)
public interface StaticLimitChecker extends LimitChecker {
  StaticLimit getLimit();
}
