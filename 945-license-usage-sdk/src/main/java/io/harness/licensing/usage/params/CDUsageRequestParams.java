package io.harness.licensing.usage.params;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.licensing.beans.modules.types.CDLicenseType;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@OwnedBy(HarnessTeam.CDC)
@Data
@SuperBuilder
@NoArgsConstructor
public class CDUsageRequestParams extends UsageRequestParams {
  CDLicenseType cdLicenseType;
}
