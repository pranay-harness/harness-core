package io.harness.cvng.core.beans;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DatadogMetricHealthDefinition {
    String dashboardName;
    String query;
    String metricName;
    String aggregation;
    String serviceInstanceTag;
    boolean isManualQuery;
    RiskProfile riskProfile;

    @JsonProperty(value = "isManualQuery")
    public boolean isManualQuery() {
        return isManualQuery;
    }
}
