package io.harness.ccm.license;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Duration;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CeLicenseInfo {
  @JsonIgnore public static final int CE_TRIAL_GRACE_PERIOD_DAYS = 15;

  private CeLicenseType licenseType;
  private long expiryTime;

  @JsonIgnore
  public long getExpiryTimeWithGracePeriod() {
    return expiryTime + Duration.ofDays(CE_TRIAL_GRACE_PERIOD_DAYS).toMillis();
  }

  @JsonIgnore
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
