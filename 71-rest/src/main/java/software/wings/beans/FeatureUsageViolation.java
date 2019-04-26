package software.wings.beans;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import lombok.Value;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.licensing.violations.RestrictedFeature;

import java.util.List;

@Getter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class FeatureUsageViolation extends FeatureViolation {
  private final int usageCount;
  @NonNull private final List<Usage> usages;

  @Builder
  public FeatureUsageViolation(RestrictedFeature restrictedFeature, List<Usage> usages) {
    super(restrictedFeature, Category.RESTRICTED_FEATURE_USAGE);
    this.usageCount = usages.size();
    this.usages = usages;
  }

  @Value
  public static class Usage {
    @NonNull @NotEmpty String entityId;
    @NonNull @NotEmpty EntityType entityType;
    @NonNull @NotEmpty String entityName;
  }
}
