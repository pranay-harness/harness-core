package software.wings.service.impl.pipeline;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.EntityType;
import software.wings.beans.Pipeline;
import software.wings.beans.PipelineStage;
import software.wings.beans.PipelineStage.PipelineStageElement;
import software.wings.beans.Variable;
import software.wings.beans.Workflow;
import software.wings.sm.StateType;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@OwnedBy(CDC)
@Slf4j
public class PipelineServiceHelper {
  private PipelineServiceHelper() {}

  public static void updateLoopingInfo(
      PipelineStage pipelineStage, Workflow workflow, List<String> infraDefinitionIds) {
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
    PipelineStageElement pipelineStageElement = pipelineStage.getPipelineStageElements().get(0);
    String infraVarNameInPipelineStage = infraDefVariables.get(0).getName();
    String infraValueInPipelineStage = pipelineStageElement.getWorkflowVariables().get(infraVarNameInPipelineStage);
    if (EmptyPredicate.isEmpty(infraValueInPipelineStage)) {
      throw new InvalidRequestException(
          "No value supplied in pipeline for infra variable: " + infraVarNameInPipelineStage);
    }
    if (infraValueInPipelineStage.contains(",")) {
      pipelineStage.setLooped(true);
      pipelineStage.setLoopedVarName(infraVarNameInPipelineStage);
      List<String> infraIdsInLoop = Arrays.asList(infraValueInPipelineStage.split(","));
      infraIdsInLoop.stream().filter(infraId -> !infraDefinitionIds.contains(infraId)).forEach(infraDefinitionIds::add);
    }
  }

  public static void updatePipelineWithLoopedState(Pipeline pipeline) {
    for (PipelineStage pipelineStage : pipeline.getPipelineStages()) {
      if (pipelineStage.isLooped()) {
        PipelineStageElement pse = pipelineStage.getPipelineStageElements().get(0);
        String varLooped = pipelineStage.getLoopedVarName();
        Map<String, String> pipelineStageVariableValues = pse.getWorkflowVariables();
        if (EmptyPredicate.isEmpty(pipelineStageVariableValues) || !pipelineStageVariableValues.containsKey(varLooped)
            || !pipelineStageVariableValues.get(varLooped).contains(",")) {
          throw new InvalidRequestException("Pipeline stage marked as loop, but doesnt have looping config");
        }
        List<String> loopedValues = Arrays.asList(pipelineStageVariableValues.get(varLooped).split(","));
        pse.setType(StateType.ENV_LOOP_STATE.getType());
        pse.getProperties().put("loopedValues", loopedValues);
        pse.getProperties().put("loopedVarName", varLooped);
      }
    }
  }
}
