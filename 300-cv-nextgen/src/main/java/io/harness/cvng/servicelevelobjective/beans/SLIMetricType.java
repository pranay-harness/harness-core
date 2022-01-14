package io.harness.cvng.servicelevelobjective.beans;

import io.harness.cvng.servicelevelobjective.entities.RatioServiceLevelIndicator.RatioServiceLevelIndicatorKeys;
import io.harness.cvng.servicelevelobjective.entities.ThresholdServiceLevelIndicator.ThresholdServiceLevelIndicatorKeys;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Arrays;
import java.util.List;
import lombok.Getter;

public enum SLIMetricType {
  @JsonProperty("Threshold") THRESHOLD(ThresholdServiceLevelIndicatorKeys.metric1),
  @JsonProperty("Ratio") RATIO(RatioServiceLevelIndicatorKeys.metric1, RatioServiceLevelIndicatorKeys.metric2);

  @Getter List<String> metricDbFields;

  SLIMetricType(String... metricDbFields) {
    this.metricDbFields = Arrays.asList(metricDbFields);
  }
}
