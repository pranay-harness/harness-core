package software.wings.verification.datadog;

import software.wings.verification.log.LogsCVConfigurationYaml;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonPropertyOrder({"type", "harnessApiVersion"})
public final class DatadogLogCVConfigurationYaml extends LogsCVConfigurationYaml {
  private String hostnameField;
}
