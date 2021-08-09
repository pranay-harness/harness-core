package software.wings.features.api;

import java.util.Map;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;

@Value
@Builder
public class FeatureUsageComplianceReport {
  @NonNull String featureName;
  @NonNull @Singular Map<String, Object> properties;
}
