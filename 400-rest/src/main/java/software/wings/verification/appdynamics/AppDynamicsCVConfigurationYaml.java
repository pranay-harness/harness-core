package software.wings.verification.appdynamics;

import software.wings.verification.CVConfiguration;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * The type Yaml.
 */
@Data
@JsonPropertyOrder({"type", "harnessApiVersion"})
@Builder
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public final class AppDynamicsCVConfigurationYaml extends CVConfiguration.CVConfigurationYaml {
  private String appDynamicsApplicationName;
  private String tierName;
}
