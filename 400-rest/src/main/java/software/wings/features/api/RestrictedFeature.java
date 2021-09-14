/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.features.api;

public interface RestrictedFeature extends Feature {
  Restrictions getRestrictionsForAccount(String accountId);

  Restrictions getRestrictions(String accountType);

  boolean isUsageCompliantWithRestrictions(String accountId, String targetAccountType);

  FeatureUsageComplianceReport getUsageComplianceReport(String accountId, String targetAccountType);
}
