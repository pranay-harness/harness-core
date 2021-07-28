package io.harness.cvng.core.beans;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PrometheusMetricDefinition {
  private String query;
  @JsonIgnore private String serviceIdentifier;
  @JsonIgnore private String envIdentifier;
  private boolean isManualQuery;
  private String groupName;
  private String metricName;
  private String serviceInstanceFieldName;
  private String prometheusMetric;
  private List<PrometheusFilter> serviceFilter;
  private List<PrometheusFilter> envFilter;
  private List<PrometheusFilter> additionalFilters;
  private String aggregation;
  RiskProfile riskProfile;

  @JsonProperty(value = "isManualQuery")
  public boolean isManualQuery() {
    return isManualQuery;
  }

  @Data
  @Builder
  public static class PrometheusFilter {
    private String labelName;
    private String labelValue;

    @JsonIgnore
    public String getQueryFilterString() {
      return labelName + "=\"" + labelValue + "\"";
    }
  }
}
