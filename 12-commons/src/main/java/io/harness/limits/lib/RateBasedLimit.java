package io.harness.limits.lib;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import java.util.concurrent.TimeUnit;

/**
 * If count = 5, duration = 1, and timeUnit = TimeUnit.SECONDS then it translates to
 * "a particular action is allowed 5 times per second"
 */
@OwnedBy(PL)
public interface RateBasedLimit extends Limit {
  int getCount();

  int getDuration();

  TimeUnit getDurationUnit();
}
