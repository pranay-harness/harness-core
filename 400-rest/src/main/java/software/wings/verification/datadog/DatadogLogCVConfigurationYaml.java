package software.wings.verification.datadog;

import static software.wings.verification.log.LogsCVConfiguration.LogsCVConfigurationYaml;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@TargetModule(HarnessModule._870_CG_YAML_BEANS)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonPropertyOrder({"type", "harnessApiVersion"})
public final class DatadogLogCVConfigurationYaml extends LogsCVConfigurationYaml {
  private String hostnameField;
}
