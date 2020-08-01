package io.harness.ccm.license;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CeLicenseInfo {
  private CeLicenseType licenseType;
  private long expiryTime;
}
