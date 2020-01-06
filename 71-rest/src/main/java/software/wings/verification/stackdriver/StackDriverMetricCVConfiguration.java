package software.wings.verification.stackdriver;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import software.wings.metrics.MetricType;
import software.wings.metrics.TimeSeriesMetricDefinition;
import software.wings.verification.CVConfiguration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants(innerTypeName = "StackDriverMetricCVConfigurationKeys")
@EqualsAndHashCode(callSuper = true)
public class StackDriverMetricCVConfiguration extends CVConfiguration {
  private List<StackDriverMetricDefinition> metricDefinitions;

  public void setMetricFilters() {
    this.metricDefinitions.forEach(StackDriverMetricDefinition::extractJson);
  }

  @Override
  public CVConfiguration deepCopy() {
    StackDriverMetricCVConfiguration clonedConfig = new StackDriverMetricCVConfiguration();
    super.copy(clonedConfig);
    clonedConfig.setMetricDefinitions(this.getMetricDefinitions());
    clonedConfig.setMetricFilters();
    return clonedConfig;
  }

  /**
   * The type Yaml.
   */
  @Data
  @JsonPropertyOrder({"type", "harnessApiVersion"})
  @Builder
  @AllArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  public static final class StackDriverMetricCVConfigurationYaml extends CVConfigurationYaml {
    private List<StackDriverMetricDefinition> metricDefinitions;
  }

  public Map<String, TimeSeriesMetricDefinition> fetchMetricTemplate() {
    if (isEmpty(metricDefinitions)) {
      return null;
    }

    Map<String, TimeSeriesMetricDefinition> rv = new HashMap<>();
    metricDefinitions.forEach(timeSeries
        -> rv.put(timeSeries.getMetricName(),
            TimeSeriesMetricDefinition.builder()
                .metricName(timeSeries.getMetricName())
                .metricType(MetricType.valueOf(timeSeries.getMetricType()))
                .build()));
    return rv;
  }
}
