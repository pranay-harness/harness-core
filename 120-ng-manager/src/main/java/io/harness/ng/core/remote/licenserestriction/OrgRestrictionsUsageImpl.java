package io.harness.ng.core.remote.licenserestriction;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.enforcement.beans.metadata.StaticLimitRestrictionMetadataDTO;
import io.harness.enforcement.client.usage.RestrictionUsageInterface;
import io.harness.ng.core.services.OrganizationService;

import com.google.inject.Inject;

@OwnedBy(HarnessTeam.PL)
public class OrgRestrictionsUsageImpl implements RestrictionUsageInterface<StaticLimitRestrictionMetadataDTO> {
  @Inject private OrganizationService organizationService;

  @Override
  public long getCurrentValue(String accountIdentifier, StaticLimitRestrictionMetadataDTO restrictionMetadataDTO) {
    return organizationService.countOrgs(accountIdentifier);
  }
}
