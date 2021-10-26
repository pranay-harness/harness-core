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
public class RatioSLIMetricSpec extends SLIMetricSpec {
  @NonNull String eventType;
  @NonNull String metric1;
  @NonNull String metric2;

  @Override
  public SLIMetricType getType() {
    return SLIMetricType.RATIO;
  }

  @Override
  public String getMetricName() {
    return metric1;
  }
}
