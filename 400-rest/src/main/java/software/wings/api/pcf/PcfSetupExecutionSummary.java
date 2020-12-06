package software.wings.api.pcf;

import software.wings.sm.StepExecutionSummary;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
public class PcfSetupExecutionSummary extends StepExecutionSummary {
  private String organization;
  private String space;
  private int maxInstanceCount;
}
