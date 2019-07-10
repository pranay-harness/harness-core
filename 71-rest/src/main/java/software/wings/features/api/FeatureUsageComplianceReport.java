package software.wings.features.api;

import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;

import java.util.Map;

@Value
@Builder
public class FeatureUsageComplianceReport {
  @NonNull String featureName;
  @NonNull @Singular Map<String, Object> properties;
}
