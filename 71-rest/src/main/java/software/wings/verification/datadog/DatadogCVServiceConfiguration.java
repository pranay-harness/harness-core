package software.wings.verification.datadog;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.github.reinert.jjschema.Attributes;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.verification.CVConfiguration;

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
  @Attributes(required = true, title = "Metrics") private String metrics;
  @Attributes(required = true, title = "Application Filter") private String applicationFilter;

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
    private String metrics;
    private String applicationFilter;

    public DatadogCVConfigurationYaml(String type, String harnessApiVersion, String name, String accountId,
        String connectorId, String envId, String serviceId, String stateType, String analysisTolerance,
        String datadogServiceName, String metrics, String applicationFilter) {
      super(type, harnessApiVersion, name, accountId, connectorId, envId, serviceId, analysisTolerance);
      this.datadogServiceName = datadogServiceName;
      this.metrics = metrics;
      this.applicationFilter = applicationFilter;
    }
  }
}
