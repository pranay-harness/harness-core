package io.harness.ccm.license;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Duration;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(CE)
public class CeLicenseInfo {
  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) public static final int CE_TRIAL_GRACE_PERIOD_DAYS = 15;

  private CeLicenseType licenseType;
  private long expiryTime;

  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  public long getExpiryTimeWithGracePeriod() {
    return expiryTime + Duration.ofDays(CE_TRIAL_GRACE_PERIOD_DAYS).toMillis();
  }

  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  public boolean isValidLicenceType() {
    switch (licenseType) {
      case LIMITED_TRIAL:
      case FULL_TRIAL:
      case PAID:
        return true;
      default:
        return false;
    }
  }
}
