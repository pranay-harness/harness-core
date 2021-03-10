package software.wings.verification.datadog;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import software.wings.verification.log.LogsCVConfigurationYaml;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@TargetModule(Module._870_CG_YAML_BEANS)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonPropertyOrder({"type", "harnessApiVersion"})
public final class DatadogLogCVConfigurationYaml extends LogsCVConfigurationYaml {
  private String hostnameField;
}
