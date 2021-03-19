package software.wings.sm.states.spotinst;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;

import software.wings.sm.StepExecutionSummary;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@OwnedBy(CDP)
public class SpotinstDeployExecutionSummary extends StepExecutionSummary {
  private String oldElastigroupId;
  private String oldElastigroupName;
  private String newElastigroupId;
  private String newElastigroupName;
}
