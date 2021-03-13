package software.wings.verification.log;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * The type Yaml.
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonPropertyOrder({"type", "harnessApiVersion"})
public final class BugsnagCVConfigurationYaml extends LogsCVConfiguration.LogsCVConfigurationYaml {
  private String orgName;
  private String projectName;
  private String releaseStage;
  private boolean browserApplication;
}
