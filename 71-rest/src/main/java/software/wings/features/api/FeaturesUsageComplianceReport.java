package software.wings.features.api;

import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;

import java.util.Collection;

@Value
@Builder
public class FeaturesUsageComplianceReport {
  @NonNull String accountId;
  @NonNull String targetAccountType;
  @NonNull @Singular Collection<FeatureUsageComplianceReport> featureUsageComplianceReports;
}
