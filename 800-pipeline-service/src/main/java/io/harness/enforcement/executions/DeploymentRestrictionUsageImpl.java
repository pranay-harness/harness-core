package io.harness.enforcement.executions;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.enforcement.beans.metadata.RateLimitRestrictionMetadataDTO;
import io.harness.enforcement.client.usage.RestrictionUsageInterface;

@OwnedBy(HarnessTeam.PIPELINE)
public class DeploymentRestrictionUsageImpl implements RestrictionUsageInterface<RateLimitRestrictionMetadataDTO> {
  @Override
  public long getCurrentValue(String accountIdentifier, RateLimitRestrictionMetadataDTO restrictionMetadataDTO) {
    // Todo: Sync up with the dashboard team to get the actual count
    return 0;
  }
}
