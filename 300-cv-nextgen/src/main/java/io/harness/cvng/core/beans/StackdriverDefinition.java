package io.harness.cvng.core.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Objects;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class StackdriverDefinition extends HealthSourceMetricDefinition {
  private String dashboardName;
  private String dashboardPath;
  private String metricName;
  private Object jsonMetricDefinition;
  private List<String> metricTags;
  private boolean isManualQuery;
  private String serviceInstanceField;
  @JsonProperty(value = "isManualQuery")
  public boolean isManualQuery() {
    return isManualQuery;
  }

  public String getServiceInstanceField() {
    if (Objects.nonNull(analysis) && Objects.nonNull(analysis.deploymentVerification)
        && Objects.nonNull(analysis.getDeploymentVerification().getServiceInstanceFieldName())) {
      return analysis.getDeploymentVerification().getServiceInstanceFieldName();
    }
    return serviceInstanceField;
  }
}
