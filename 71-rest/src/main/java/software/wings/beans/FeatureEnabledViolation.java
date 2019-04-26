package software.wings.beans;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import software.wings.licensing.violations.RestrictedFeature;

@Getter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class FeatureEnabledViolation extends FeatureViolation {
  private final int usageCount;

  @Builder
  public FeatureEnabledViolation(RestrictedFeature restrictedFeature, int usageCount) {
    super(restrictedFeature, Category.RESTRICTED_FEATURE_ENABLED);
    this.usageCount = usageCount;
  }
}
