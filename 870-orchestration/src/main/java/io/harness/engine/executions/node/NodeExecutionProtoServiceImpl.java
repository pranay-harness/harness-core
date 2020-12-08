package io.harness.engine.executions.node;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.execution.NodeExecution;
import io.harness.execution.NodeExecutionMapper;
import io.harness.pms.execution.NodeExecutionProto;
import io.harness.pms.sdk.core.steps.Step;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.sdk.registries.StepRegistry;
import io.harness.pms.serializer.json.JsonOrchestrationUtils;
import io.harness.pms.steps.StepType;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(CDC)
@Slf4j
public class NodeExecutionProtoServiceImpl implements NodeExecutionProtoService {
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private StepRegistry stepRegistry;

  @Override
  public NodeExecution save(NodeExecutionProto nodeExecution) {
    return nodeExecutionService.save(NodeExecutionMapper.fromNodeExecutionProto(nodeExecution));
  }

  @Override
  public StepParameters extractStepParameters(NodeExecutionProto nodeExecution) {
    return extractStepParametersInternal(
        nodeExecution.getNode().getStepType(), nodeExecution.getNode().getStepParameters());
  }

  @Override
  public StepParameters extractResolvedStepParameters(NodeExecutionProto nodeExecution) {
    return extractStepParametersInternal(
        nodeExecution.getNode().getStepType(), nodeExecution.getResolvedStepParameters());
  }

  private StepParameters extractStepParametersInternal(StepType stepType, String stepParameters) {
    Step<?> step = stepRegistry.obtain(stepType);
    if (isEmpty(stepParameters)) {
      return null;
    }
    return JsonOrchestrationUtils.asObject(stepParameters, step.getStepParametersClass());
  }
}
