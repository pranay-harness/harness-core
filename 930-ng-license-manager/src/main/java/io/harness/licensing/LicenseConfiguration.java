package io.harness.licensing;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LicenseConfiguration {
  private long accountLicenseCheckJobFrequencyInMinutes;
}
