package software.wings.verification.datadog;

import static io.harness.annotations.dev.HarnessTeam.CV;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.sm.states.DatadogState;
import software.wings.verification.CVConfigurationYaml;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * The type Yaml.
 */
@TargetModule(HarnessModule._955_CG_YAML)
@Data
@Builder
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonPropertyOrder({"type", "harnessApiVersion"})
@OwnedBy(CV)
public final class DatadogCVConfigurationYaml extends CVConfigurationYaml {
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

    public DatadogState.Metric convertToDatadogMetric() {
      Set<String> tagList = this.getTags() == null ? null : new HashSet<>(this.getTags());
      return DatadogState.Metric.builder()
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

    public static YamlMetric convertToYamlMetric(DatadogState.Metric metric) {
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
