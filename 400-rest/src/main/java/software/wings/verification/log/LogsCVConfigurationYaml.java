package software.wings.verification.log;

import software.wings.verification.CVConfigurationYaml;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonPropertyOrder({"type", "harnessApiVersion"})
public class LogsCVConfigurationYaml extends CVConfigurationYaml {
  private String query;
  private Long baselineStartMinute;
  private Long baselineEndMinute;
  private String alertPriority;
}
