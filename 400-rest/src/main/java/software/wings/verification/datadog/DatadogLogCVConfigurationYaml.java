package software.wings.verification.datadog;

import static io.harness.annotations.dev.HarnessTeam.CV;

import static software.wings.verification.log.LogsCVConfiguration.LogsCVConfigurationYaml;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@TargetModule(HarnessModule._955_CG_YAML)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonPropertyOrder({"type", "harnessApiVersion"})
@OwnedBy(CV)
public final class DatadogLogCVConfigurationYaml extends LogsCVConfigurationYaml {
  private String hostnameField;
}
