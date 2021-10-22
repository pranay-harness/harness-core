package io.harness.cvng.core.beans;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class DatadogMetricHealthDefinition {
    String dashboardName;
    RiskProfile riskProfile;
    String query;
    String metricName;
    List<String> metricTags;
    boolean isManualQuery;
    String serviceInstanceField;
    @JsonProperty(value = "isManualQuery")
    public boolean isManualQuery() {
        return isManualQuery;
    }
}
