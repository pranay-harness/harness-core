package io.harness.cvng.core.beans;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
public class DatadogMetricHealthDefinition extends HealthSourceMetricDefinition {
  String dashboardId;
  String dashboardName;
  String query;
  String groupingQuery;
  String metric;
  String aggregation;
  String serviceInstanceIdentifierTag;
  List<String> metricTags;
  boolean isManualQuery;
  RiskProfile riskProfile;

  @JsonProperty(value = "isManualQuery")
  public boolean isManualQuery() {
    return isManualQuery;
  }
}
