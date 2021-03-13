package software.wings.verification.log;

import static software.wings.verification.log.LogsCVConfiguration.*;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * The type Yaml.
 */
@TargetModule(Module._870_CG_YAML_BEANS)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonPropertyOrder({"type", "harnessApiVersion"})
public final class BugsnagCVConfigurationYaml extends LogsCVConfigurationYaml {
  private String orgName;
  private String projectName;
  private String releaseStage;
  private boolean browserApplication;
}
