package software.wings.verification.log;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonPropertyOrder({"type", "harnessApiVersion"})
public class StackdriverCVConfigurationYaml extends LogsCVConfigurationYaml {
  private String hostnameField;
  private String messageField;
  private boolean isLogsConfiguration;
}
