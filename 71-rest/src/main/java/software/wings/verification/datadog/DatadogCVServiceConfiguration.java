package software.wings.verification.datadog;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.github.reinert.jjschema.Attributes;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.sm.states.DatadogState.Metric;
import software.wings.verification.CVConfiguration;

import java.util.Map;
import java.util.Set;

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
    private Map<String, Set<Metric>> customMetrics;
  }
}
