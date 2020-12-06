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
