/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.security.authentication.totp;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.limits.impl.model.RateLimit;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.inject.Singleton;
import java.util.concurrent.TimeUnit;
import lombok.Builder;

@Singleton
@Builder
@OwnedBy(HarnessTeam.PL)
public class TotpLimit {
  private int count;
  private int duration;
  private TimeUnit durationUnit;

  @JsonProperty
  public void setCount(int count) {
    this.count = count;
  }

  @JsonProperty
  public void setDuration(int duration) {
    this.duration = duration;
  }

  @JsonProperty
  public void setDurationUnit(String durationUnit) {
    this.durationUnit = TimeUnit.valueOf(durationUnit);
  }

  public RateLimit getRateLimit() {
    return new RateLimit(count, duration, durationUnit);
  }
}
