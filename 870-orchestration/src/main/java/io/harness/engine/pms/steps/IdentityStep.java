package io.harness.engine.pms.steps;

import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.execution.NodeExecution;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.ChildExecutableResponse;
import io.harness.pms.contracts.execution.ChildrenExecutableResponse;
import io.harness.pms.sdk.core.steps.executables.ChildExecutable;
import io.harness.pms.sdk.core.steps.executables.ChildrenExecutable;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.tasks.ResponseData;

import com.google.inject.Inject;
import java.util.Map;

public class IdentityStep
    implements ChildExecutable<IdentityStepParameters>, ChildrenExecutable<IdentityStepParameters> {
  @Inject private NodeExecutionService nodeExecutionService;

  @Override
  public ChildExecutableResponse obtainChild(
      Ambiance ambiance, IdentityStepParameters identityParams, StepInputPackage inputPackage) {
    NodeExecution originalNodeExecution = nodeExecutionService.get(identityParams.getOriginalNodeExecutionId());
    // TODO: Copy the outputs here
    return originalNodeExecution.getExecutableResponses().get(0).getChild();
  }

  @Override
  public StepResponse handleChildResponse(
      Ambiance ambiance, IdentityStepParameters identityParams, Map<String, ResponseData> responseDataMap) {
    NodeExecution originalNodeExecution = nodeExecutionService.get(identityParams.getOriginalNodeExecutionId());
    // Get Clones  Outcomes here
    // Clone outcomes
    // Right now we should send empty from here but handle on the strategy to attach appropriate refs
    // We can remove the refs to get rid of this use case
    return StepResponse.builder().status(originalNodeExecution.getStatus()).build();
  }

  @Override
  public ChildrenExecutableResponse obtainChildren(
      Ambiance ambiance, IdentityStepParameters identityParams, StepInputPackage inputPackage) {
    NodeExecution originalNodeExecution = nodeExecutionService.get(identityParams.getOriginalNodeExecutionId());
    // TODO: Copy the outputs here
    return originalNodeExecution.getExecutableResponses().get(0).getChildren();
  }

  @Override
  public StepResponse handleChildrenResponse(
      Ambiance ambiance, IdentityStepParameters identityParams, Map<String, ResponseData> responseDataMap) {
    NodeExecution originalNodeExecution = nodeExecutionService.get(identityParams.getOriginalNodeExecutionId());
    // Get Clones  Outcomes here
    // Clone outcomes
    // Right now we should send send from here but handle on the strategy to attach appropriate refs
    // We can remove the refs to get rid of this use case
    return StepResponse.builder().status(originalNodeExecution.getStatus()).build();
  }

  @Override
  public Class<IdentityStepParameters> getStepParametersClass() {
    return IdentityStepParameters.class;
  }
}
