package software.wings.beans;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@Builder
@EqualsAndHashCode
public class FailureCriteria {
  @Min(value = 0) @Max(value = 100) private int failureThresholdPercentage;
}
