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
