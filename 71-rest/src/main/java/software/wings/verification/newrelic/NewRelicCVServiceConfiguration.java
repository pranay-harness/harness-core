package software.wings.verification.newrelic;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.github.reinert.jjschema.Attributes;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.verification.CVConfiguration;

import java.util.List;

/**
 * @author Vaibhav Tulsyan
 * 05/Oct/2018
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class NewRelicCVServiceConfiguration extends CVConfiguration {
  @Attributes(required = true, title = "Application Name") private String applicationId;
  @Attributes(required = true, title = "Metrics") private List<String> metrics;

  @Override
  public CVConfiguration deepCopy() {
    NewRelicCVServiceConfiguration clonedConfig = new NewRelicCVServiceConfiguration();
    super.copy(clonedConfig);
    clonedConfig.setApplicationId(this.getApplicationId());
    clonedConfig.setMetrics(this.getMetrics());
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
  public static final class NewRelicCVConfigurationYaml extends CVConfigurationYaml {
    private String newRelicApplicationName;
    private List<String> metrics;
  }
}
