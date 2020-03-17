package software.wings.verification.datadog;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.verification.CVConfiguration;
import software.wings.verification.log.LogsCVConfiguration;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class DatadogLogCVConfiguration extends LogsCVConfiguration {
  private String hostnameField;

  @Override
  public CVConfiguration deepCopy() {
    DatadogLogCVConfiguration clonedConfig = new DatadogLogCVConfiguration();
    super.copy(clonedConfig);
    clonedConfig.setHostnameField(this.getHostnameField());
    return clonedConfig;
  }
  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  @JsonPropertyOrder({"type", "harnessApiVersion"})
  public static final class DatadogLogCVConfigurationYaml extends LogsCVConfigurationYaml {
    private String hostnameField;
  }
}
