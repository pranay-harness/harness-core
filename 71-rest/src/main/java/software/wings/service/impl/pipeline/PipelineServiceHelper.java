package software.wings.service.impl.pipeline;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static software.wings.sm.StateType.ENV_LOOP_STATE;
import static software.wings.sm.StateType.ENV_STATE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;

import software.wings.beans.EntityType;
import software.wings.beans.Pipeline;
import software.wings.beans.PipelineStage;
import software.wings.beans.PipelineStage.PipelineStageElement;
import software.wings.beans.RuntimeInputsConfig;
import software.wings.beans.Variable;
import software.wings.beans.Workflow;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(CDC)
@Slf4j
public class PipelineServiceHelper {
  private PipelineServiceHelper() {}

  public static void updateLoopingInfo(PipelineStage pipelineStage, Workflow workflow, List<String> infraDefinitionIds,
      boolean isRuntimeVariableEnabled) {
    PipelineStageElement pipelineStageElement = pipelineStage.getPipelineStageElements().get(0);
    if (pipelineStageElement.checkDisableAssertion()) {
      return;
    }
    List<Variable> userVariables = workflow.getOrchestrationWorkflow().getUserVariables();
    if (EmptyPredicate.isEmpty(userVariables)) {
      return;
    }
    List<Variable> infraDefVariables = userVariables.stream()
                                           .filter(t -> EntityType.INFRASTRUCTURE_DEFINITION == t.obtainEntityType())
                                           .collect(Collectors.toList());
    if (EmptyPredicate.isEmpty(infraDefVariables) || infraDefVariables.size() > 1) {
      return;
    }

    RuntimeInputsConfig runtimeInputsConfig = pipelineStageElement.getRuntimeInputsConfig();
    String infraVarNameInPipelineStage = infraDefVariables.get(0).getName();

    if (isRuntimeVariableEnabled && runtimeInputsConfig != null
        && isNotEmpty(runtimeInputsConfig.getRuntimeInputVariables())
        && runtimeInputsConfig.getRuntimeInputVariables().contains(infraVarNameInPipelineStage)) {
      pipelineStage.setLooped(true);
      pipelineStage.setLoopedVarName(infraVarNameInPipelineStage);
      return;
    }

    String infraValueInPipelineStage = pipelineStageElement.getWorkflowVariables().get(infraVarNameInPipelineStage);
    if (EmptyPredicate.isEmpty(infraValueInPipelineStage)) {
      throw new InvalidRequestException(
          "No value supplied in pipeline for infra variable: " + infraVarNameInPipelineStage);
    }
    if (infraValueInPipelineStage.contains(",")) {
      pipelineStage.setLooped(true);
      pipelineStage.setLoopedVarName(infraVarNameInPipelineStage);
      List<String> infraIdsInLoop = Arrays.asList(infraValueInPipelineStage.trim().split("\\s*,\\s*"));
      infraIdsInLoop.stream().filter(infraId -> !infraDefinitionIds.contains(infraId)).forEach(infraDefinitionIds::add);
    }
  }

  public static void updatePipelineWithLoopedState(Pipeline pipeline, boolean isRuntimeEnabled) {
    for (PipelineStage pipelineStage : pipeline.getPipelineStages()) {
      if (pipelineStage.isLooped()) {
        PipelineStageElement pse = pipelineStage.getPipelineStageElements().get(0);
        if (ENV_STATE.getType().equals(pse.getType())) {
          String varLooped = pipelineStage.getLoopedVarName();
          Map<String, String> pipelineStageVariableValues = pse.getWorkflowVariables();
          RuntimeInputsConfig runtimeInputsConfig = pse.getRuntimeInputsConfig();
          List<String> runtimeVariablesInStage =
              runtimeInputsConfig != null ? runtimeInputsConfig.getRuntimeInputVariables() : new ArrayList<>();

          List<String> loopedValues = new ArrayList<>();
          // In case an Infra var is marked runtime, we assume the stage to be looped.
          // The default value can be empty, or a single value as well.
          if (isRuntimeEnabled && isNotEmpty(runtimeVariablesInStage) && runtimeVariablesInStage.contains(varLooped)) {
            if (isNotEmpty(pipelineStageVariableValues) && pipelineStageVariableValues.containsKey(varLooped)) {
              String defaultValue = pipelineStageVariableValues.get(varLooped);
              if (isNotEmpty(defaultValue)) {
                loopedValues = Arrays.asList(pipelineStageVariableValues.get(varLooped).trim().split("\\s*,\\s*"));
              }
            }
          } else {
            if (EmptyPredicate.isEmpty(pipelineStageVariableValues)
                || !pipelineStageVariableValues.containsKey(varLooped)
                || !pipelineStageVariableValues.get(varLooped).contains(",")) {
              throw new InvalidRequestException("Pipeline stage marked as loop, but doesnt have looping config");
            }
            loopedValues = Arrays.asList(pipelineStageVariableValues.get(varLooped).trim().split("\\s*,\\s*"));
          }
          pse.setType(ENV_LOOP_STATE.getType());
          pse.getProperties().put("loopedValues", loopedValues);
          pse.getProperties().put("loopedVarName", varLooped);
        }
      }
    }
  }
}
