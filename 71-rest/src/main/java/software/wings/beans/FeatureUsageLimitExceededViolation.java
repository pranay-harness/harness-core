package software.wings.beans;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import software.wings.licensing.violations.RestrictedFeature;

@Getter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class FeatureUsageLimitExceededViolation extends FeatureViolation {
  private final int paidLicenseUsageLimit;
  private final int usageLimit;
  private final int usageCount;

  @Builder
  public FeatureUsageLimitExceededViolation(
      RestrictedFeature restrictedFeature, int paidLicenseUsageLimit, int usageLimit, int usageCount) {
    super(restrictedFeature, Category.RESTRICTED_FEATURE_USAGE_LIMIT_EXCEEDED);
    this.paidLicenseUsageLimit = paidLicenseUsageLimit;
    this.usageLimit = usageLimit;
    this.usageCount = usageCount;
  }
}
