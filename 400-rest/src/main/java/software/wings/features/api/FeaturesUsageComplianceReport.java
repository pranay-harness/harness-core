/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.features.api;

import java.util.Collection;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;

@Value
@Builder
public class FeaturesUsageComplianceReport {
  @NonNull String accountId;
  @NonNull String targetAccountType;
  @NonNull @Singular Collection<FeatureUsageComplianceReport> featureUsageComplianceReports;
}
