package software.wings.verification.appdynamics;

import static software.wings.verification.CVConfiguration.CVConfigurationYaml;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * The type Yaml.
 */
@TargetModule(Module._870_CG_YAML_BEANS)
@Data
@JsonPropertyOrder({"type", "harnessApiVersion"})
@Builder
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public final class AppDynamicsCVConfigurationYaml extends CVConfigurationYaml {
  private String appDynamicsApplicationName;
  private String tierName;
}
