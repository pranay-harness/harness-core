package io.harness.feature.client.custom;

import io.harness.feature.beans.FeatureDetailsDTO;
import io.harness.feature.constants.FeatureRestriction;
import io.harness.licensing.beans.summary.LicensesWithSummaryDTO;

public interface CustomFeatureRestriction<T extends CustomRestrictionResult, K extends LicensesWithSummaryDTO> {
  T evaluateCustomRestriction(FeatureRestriction featureName, String accountIdentifier, K licenseSummary);

  FeatureDetailsDTO toFeatureDetailsDTO(T restrictionResult);
}
