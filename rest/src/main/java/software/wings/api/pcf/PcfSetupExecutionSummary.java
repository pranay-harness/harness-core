package software.wings.api.pcf;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.sm.StepExecutionSummary;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
public class PcfSetupExecutionSummary extends StepExecutionSummary {
  private String organization;
  private String space;
  private int maxInstanceCount;
}
