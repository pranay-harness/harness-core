package io.harness.cdng.licenserestriction;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.enforcement.beans.metadata.RateLimitRestrictionMetadataDTO;
import io.harness.enforcement.client.usage.RestrictionUsageInterface;

@OwnedBy(HarnessTeam.CDP)
public class ServiceRestrictionsUsageImpl implements RestrictionUsageInterface<RateLimitRestrictionMetadataDTO> {
  @Override
  public long getCurrentValue(String accountIdentifier, RateLimitRestrictionMetadataDTO restrictionMetadataDTO) {
    return 0;
    // TODO: Implement via k8s/helm service
  }
}