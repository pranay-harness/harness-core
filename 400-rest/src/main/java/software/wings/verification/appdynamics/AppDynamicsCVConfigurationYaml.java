package software.wings.verification.appdynamics;

import static io.harness.annotations.dev.HarnessTeam.CV;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.verification.MetricCVConfigurationYaml;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * The type Yaml.
 */
@TargetModule(HarnessModule._870_CG_YAML)
@Data
@JsonPropertyOrder({"type", "harnessApiVersion"})
@Builder
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@OwnedBy(CV)
public final class AppDynamicsCVConfigurationYaml extends MetricCVConfigurationYaml {
  private String appDynamicsApplicationName;
  private String tierName;
}
