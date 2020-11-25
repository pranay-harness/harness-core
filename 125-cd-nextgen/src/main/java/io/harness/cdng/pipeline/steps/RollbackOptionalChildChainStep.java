package io.harness.cdng.pipeline.steps;

import io.harness.ambiance.Ambiance;
import io.harness.cdng.pipeline.beans.RollbackNode;
import io.harness.cdng.pipeline.beans.RollbackOptionalChildChainStepParameters;
import io.harness.cdng.pipeline.plancreators.PlanCreatorHelper;
import io.harness.facilitator.PassThroughData;
import io.harness.facilitator.modes.chain.child.ChildChainExecutable;
import io.harness.facilitator.modes.chain.child.ChildChainResponse;
import io.harness.pms.execution.Status;
import io.harness.pms.steps.StepType;
import io.harness.state.io.StepInputPackage;
import io.harness.state.io.StepResponse;
import io.harness.state.io.StepResponseNotifyData;
import io.harness.steps.section.chain.SectionChainPassThroughData;
import io.harness.tasks.ResponseData;

import com.google.inject.Inject;
import java.util.Map;

public class RollbackOptionalChildChainStep implements ChildChainExecutable<RollbackOptionalChildChainStepParameters> {
  public static final StepType STEP_TYPE = StepType.newBuilder().setType("ROLLBACK_OPTIONAL_CHILD_CHAIN").build();

  @Inject private PlanCreatorHelper planCreatorHelper;

  @Override
  public Class<RollbackOptionalChildChainStepParameters> getStepParametersClass() {
    return RollbackOptionalChildChainStepParameters.class;
  }

  @Override
  public ChildChainResponse executeFirstChild(
      Ambiance ambiance, RollbackOptionalChildChainStepParameters stepParameters, StepInputPackage inputPackage) {
    int index = 0;
    for (int i = index; i < stepParameters.getChildNodes().size(); i++) {
      RollbackNode childNode = stepParameters.getChildNodes().get(i);

      if (planCreatorHelper.shouldNodeRun(childNode, ambiance)) {
        return ChildChainResponse.builder()
            .nextChildId(childNode.getNodeId())
            .passThroughData(SectionChainPassThroughData.builder().childIndex(i).build())
            .lastLink(stepParameters.getChildNodes().size() == i + 1)
            .build();
      }
    }
    return ChildChainResponse.builder().suspend(true).build();
  }

  @Override
  public ChildChainResponse executeNextChild(Ambiance ambiance, RollbackOptionalChildChainStepParameters stepParameters,
      StepInputPackage inputPackage, PassThroughData passThroughData, Map<String, ResponseData> responseDataMap) {
    int index = ((SectionChainPassThroughData) passThroughData).getChildIndex() + 1;

    for (int i = index; i < stepParameters.getChildNodes().size(); i++) {
      RollbackNode childNode = stepParameters.getChildNodes().get(i);

      if (planCreatorHelper.shouldNodeRun(childNode, ambiance)) {
        return ChildChainResponse.builder()
            .nextChildId(childNode.getNodeId())
            .passThroughData(SectionChainPassThroughData.builder().childIndex(i).build())
            .lastLink(stepParameters.getChildNodes().size() == i + 1)
            .previousChildId(responseDataMap.keySet().iterator().next())
            .build();
      }
    }
    return ChildChainResponse.builder().suspend(true).build();
  }

  @Override
  public StepResponse finalizeExecution(Ambiance ambiance, RollbackOptionalChildChainStepParameters stepParameters,
      PassThroughData passThroughData, Map<String, ResponseData> responseDataMap) {
    StepResponseNotifyData notifyData = (StepResponseNotifyData) responseDataMap.values().iterator().next();
    // If status is suspended, then we should mark the execution as success
    if (notifyData.getStatus() == Status.SUSPENDED) {
      return StepResponse.builder().status(Status.SUCCEEDED).failureInfo(notifyData.getFailureInfo()).build();
    }
    return StepResponse.builder().status(notifyData.getStatus()).failureInfo(notifyData.getFailureInfo()).build();
  }
}
