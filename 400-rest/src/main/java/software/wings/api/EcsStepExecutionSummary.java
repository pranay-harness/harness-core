package software.wings.api;

import software.wings.sm.StepExecutionSummary;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Created by rishi on 4/4/17.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@EqualsAndHashCode(callSuper = true)
public class EcsStepExecutionSummary extends StepExecutionSummary {
  private String ecsServiceName;
  private String ecsOldServiceName;
  private int instanceCount;
}
