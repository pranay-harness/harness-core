package io.harness.cvng.core.beans;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class StackdriverDefinition {
  private String dashboardName;
  private String dashboardPath;
  private String metricName;
  private Object jsonMetricDefinition;
  private List<String> metricTags;
  private RiskProfile riskProfile;
  private boolean isManualQuery;
  private String serviceInstanceField;
  @JsonProperty(value = "isManualQuery")
  public boolean isManualQuery() {
    return isManualQuery;
  }
}
