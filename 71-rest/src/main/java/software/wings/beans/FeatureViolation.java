package software.wings.beans;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import software.wings.licensing.violations.RestrictedFeature;

@RequiredArgsConstructor
@Getter
@ToString
@EqualsAndHashCode
public abstract class FeatureViolation {
  @NonNull private final RestrictedFeature restrictedFeature;
  @NonNull private final Category violationCategory;

  public enum Category { RESTRICTED_FEATURE_USAGE_LIMIT_EXCEEDED, RESTRICTED_FEATURE_ENABLED, RESTRICTED_FEATURE_USAGE }
}
