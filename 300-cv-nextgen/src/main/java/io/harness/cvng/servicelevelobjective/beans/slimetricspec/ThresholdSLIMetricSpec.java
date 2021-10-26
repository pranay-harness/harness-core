package io.harness.cvng.servicelevelobjective.beans.slimetricspec;

import io.harness.cvng.servicelevelobjective.beans.SLIMetricType;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ThresholdSLIMetricSpec extends SLIMetricSpec {
  @NonNull String metric1;

  @Override
  public SLIMetricType getType() {
    return SLIMetricType.THRESHOLD;
  }

  @Override
  public String getMetricName() {
    return metric1;
  }
}
