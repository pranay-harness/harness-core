package software.wings.beans;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

@Value
@EqualsAndHashCode
@Builder
public class FailureCriteria {
  @Min(value = 0) @Max(value = 100) private int failureThresholdPercentage;
}
