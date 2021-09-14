package io.harness.feature.handlers;

import static io.harness.remote.client.NGRestUtils.getResponse;

import io.harness.exception.InvalidRequestException;
import io.harness.feature.beans.FeatureUsageDTO;
import io.harness.feature.constants.FeatureRestriction;
import io.harness.feature.interfaces.LimitRestriction;

import lombok.experimental.UtilityClass;

@UtilityClass
public class RestrictionUtils {
  public long getCurrentUsage(
      LimitRestriction limitRestriction, FeatureRestriction featureName, String accountIdentifier) {
    try {
      FeatureUsageDTO response =
          getResponse(limitRestriction.getFeatureUsageClient().getFeatureUsage(featureName, accountIdentifier));
      return response.getCount();
    } catch (Exception e) {
      throw new InvalidRequestException(String.format("Failed to query usage data for feature [%s] and accountId [%s]",
                                            featureName.name(), accountIdentifier),
          e);
    }
  }

  public boolean isAvailable(long currentCount, long limit) {
    return currentCount < limit;
  }
}
