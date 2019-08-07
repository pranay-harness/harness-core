package software.wings.verification.log;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.github.reinert.jjschema.Attributes;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.stencils.DefaultValue;

/**
 * Created by Pranjal on 06/04/2019
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class StackdriverCVConfiguration extends LogsCVConfiguration {
  @Attributes(title = "Is Log Configuration", required = true)
  @DefaultValue("true")
  private boolean isLogsConfiguration;

  @Attributes(required = true, title = "Host Name Field") @DefaultValue("pod_id") protected String hostnameField;

  @Attributes(required = true, title = "Log Message Field")
  @DefaultValue("textPayload")
  protected String logMessageField;

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  @JsonPropertyOrder({"type", "harnessApiVersion"})
  public static class StackdriverCVConfigurationYaml extends LogsCVConfigurationYaml {
    private boolean isLogsConfiguration;
  }
}
