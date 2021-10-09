package io.harness.enforcement.handlers;

import static io.harness.remote.client.NGRestUtils.getResponse;

import io.harness.enforcement.beans.FeatureRestrictionUsageDTO;
import io.harness.enforcement.beans.metadata.RestrictionMetadataDTO;
import io.harness.enforcement.constants.FeatureRestrictionName;
import io.harness.enforcement.interfaces.LimitRestrictionInterface;
import io.harness.exception.InvalidRequestException;

import lombok.experimental.UtilityClass;

@UtilityClass
public class RestrictionUtils {
  public long getCurrentUsage(LimitRestrictionInterface limitRestriction, FeatureRestrictionName featureRestrictionName,
      String accountIdentifier, RestrictionMetadataDTO restrictionMetadataDTO) {
    try {
      FeatureRestrictionUsageDTO response = getResponse(limitRestriction.getEnforcementSdkClient().getRestrictionUsage(
          featureRestrictionName, accountIdentifier, restrictionMetadataDTO));
      return response.getCount();
    } catch (Exception e) {
      throw new InvalidRequestException(String.format("Failed to query usage data for feature [%s] and accountId [%s]",
                                            featureRestrictionName.name(), accountIdentifier),
          e);
    }
  }

  public boolean isAvailable(long currentCount, long limit) {
    return currentCount < limit;
  }
}
