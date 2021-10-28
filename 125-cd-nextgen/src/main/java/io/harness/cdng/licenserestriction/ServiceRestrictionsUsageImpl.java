package io.harness.cdng.licenserestriction;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.usage.beans.CDLicenseUsageDTO;
import io.harness.enforcement.beans.metadata.RateLimitRestrictionMetadataDTO;
import io.harness.enforcement.client.usage.RestrictionUsageInterface;
import io.harness.licensing.usage.interfaces.LicenseUsageInterface;
import io.harness.licensing.usage.params.CDUsageRequestParams;

import com.google.inject.Inject;

@OwnedBy(HarnessTeam.CDP)
public class ServiceRestrictionsUsageImpl implements RestrictionUsageInterface<RateLimitRestrictionMetadataDTO> {
  @Inject private LicenseUsageInterface<CDLicenseUsageDTO, CDUsageRequestParams> licenseUsageInterface;

  @Override
  public long getCurrentValue(String accountIdentifier, RateLimitRestrictionMetadataDTO restrictionMetadataDTO) {
    //    CDLicenseUsageDTO licenseUsage = licenseUsageInterface.getLicenseUsage(accountIdentifier, ModuleType.CD,
    //            new Date().getTime(), CDUsageRequestParams.builder().cdLicenseType(SERVICES).build());
    return 0;
    // TODO: Implement via license counts
  }
}