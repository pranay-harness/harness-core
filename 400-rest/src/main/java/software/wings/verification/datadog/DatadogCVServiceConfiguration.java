package software.wings.verification.datadog;

import software.wings.sm.states.DatadogState.Metric;
import software.wings.verification.CVConfiguration;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.github.reinert.jjschema.Attributes;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * @author Vaibhav Tulsyan
 * 16/Oct/2018
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class DatadogCVServiceConfiguration extends CVConfiguration {
  @Attributes(required = false, title = "Datadog Service Name") private String datadogServiceName;

  // comma separated metrics
  @Attributes(required = false, title = "Docker Metrics") private Map<String, String> dockerMetrics;
  // comma separated metrics
  @Attributes(required = false, title = "ECS Metrics") private Map<String, String> ecsMetrics;
  @Attributes(required = false, title = "Custom Metrics") private Map<String, Set<Metric>> customMetrics;

  @Override
  public CVConfiguration deepCopy() {
    DatadogCVServiceConfiguration clonedConfig = new DatadogCVServiceConfiguration();
    super.copy(clonedConfig);
    clonedConfig.setDatadogServiceName(this.getDatadogServiceName());
    clonedConfig.setDockerMetrics(this.getDockerMetrics());
    clonedConfig.setEcsMetrics(this.getEcsMetrics());
    clonedConfig.setCustomMetrics(this.getCustomMetrics());
    return clonedConfig;
  }

  /**
   * The type Yaml.
   */
  @Data
  @Builder
  @AllArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  @JsonPropertyOrder({"type", "harnessApiVersion"})
  public static final class DatadogCVConfigurationYaml extends CVConfigurationYaml {
    private String datadogServiceName;
    private Map<String, String> dockerMetrics;
    private Map<String, String> ecsMetrics;
    private Map<String, List<YamlMetric>> customMetrics;

    @Data
    @Builder
    public static class YamlMetric {
      private String metricName;
      private String mlMetricType;
      private String datadogMetricType;
      private String displayName;
      private String transformation;
      private String transformation24x7;
      private List<String> tags;
      private String txnName; // this field is optional. It can be extracted from the response

      public Metric convertToDatadogMetric() {
        Set<String> tagList = this.getTags() == null ? null : new HashSet<>(this.getTags());
        return Metric.builder()
            .metricName(this.getMetricName())
            .mlMetricType(this.getMlMetricType())
            .datadogMetricType(this.getDatadogMetricType())
            .displayName(this.getDisplayName())
            .transformation(this.getTransformation())
            .transformation24x7(this.getTransformation24x7())
            .tags(tagList)
            .txnName(this.getTxnName())
            .build();
      }

      public static YamlMetric convertToYamlMetric(Metric metric) {
        List<String> tagList = metric.getTags() == null ? null : new ArrayList<>(metric.getTags());
        return YamlMetric.builder()
            .metricName(metric.getMetricName())
            .mlMetricType(metric.getMlMetricType())
            .datadogMetricType(metric.getDatadogMetricType())
            .displayName(metric.getDisplayName())
            .transformation(metric.getTransformation())
            .transformation24x7(metric.getTransformation24x7())
            .tags(tagList)
            .txnName(metric.getTxnName())
            .build();
      }
    }
  }
}
