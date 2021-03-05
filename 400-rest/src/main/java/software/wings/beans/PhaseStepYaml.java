package software.wings.beans;

import software.wings.beans.workflow.StepSkipStrategyYaml;
import software.wings.yaml.BaseYamlWithType;
import software.wings.yaml.workflow.StepYaml;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public final class PhaseStepYaml extends BaseYamlWithType {
  private String name;
  private String statusForRollback;
  private boolean stepsInParallel;
  private List<StepYaml> steps = new ArrayList<>();
  private List<FailureStrategyYaml> failureStrategies = new ArrayList<>();
  private List<StepSkipStrategyYaml> stepSkipStrategies = new ArrayList<>();
  private String phaseStepNameForRollback;
  private Integer waitInterval;

  @lombok.Builder
  public PhaseStepYaml(String type, String name, String statusForRollback, boolean stepsInParallel,
      List<StepYaml> steps, List<FailureStrategyYaml> failureStrategies, List<StepSkipStrategyYaml> stepSkipStrategies,
      String phaseStepNameForRollback, Integer waitInterval) {
    super(type);
    this.name = name;
    this.statusForRollback = statusForRollback;
    this.stepsInParallel = stepsInParallel;
    this.steps = steps;
    this.failureStrategies = failureStrategies;
    this.stepSkipStrategies = stepSkipStrategies;
    this.phaseStepNameForRollback = phaseStepNameForRollback;
    this.waitInterval = waitInterval;
  }
}
