package software.wings.beans;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

@Data
@EqualsAndHashCode
@Builder
public class FailureCriteria {
  @Min(value = 0) @Max(value = 100) private int failureThresholdPercentage;
}
