/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.features.api;

import java.util.Map;

public interface FeatureService {
  FeatureRestrictions getFeatureRestrictions();

  boolean complyFeatureUsagesWithRestrictions(String accountId, Map<String, Map<String, Object>> requiredInfoToComply);

  boolean complyFeatureUsagesWithRestrictions(
      String accountId, String targetAccountType, Map<String, Map<String, Object>> requiredInfoToComply);

  FeaturesUsageComplianceReport getFeaturesUsageComplianceReport(String accountId);

  FeaturesUsageComplianceReport getFeaturesUsageComplianceReport(String accountId, String targetAccountType);
}
