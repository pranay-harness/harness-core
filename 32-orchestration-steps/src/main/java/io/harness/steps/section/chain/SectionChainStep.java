package io.harness.steps.section.chain;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.ambiance.Ambiance;
import io.harness.annotations.dev.OwnedBy;
import io.harness.facilitator.PassThroughData;
import io.harness.facilitator.modes.chain.child.ChildChainExecutable;
import io.harness.facilitator.modes.chain.child.ChildChainResponse;
import io.harness.pms.execution.Status;
import io.harness.pms.steps.StepType;
import io.harness.state.Step;
import io.harness.state.io.StepInputPackage;
import io.harness.state.io.StepResponse;
import io.harness.state.io.StepResponse.StepResponseBuilder;
import io.harness.state.io.StepResponseNotifyData;
import io.harness.steps.OrchestrationStepTypes;
import io.harness.tasks.ResponseData;

import java.util.Map;

@OwnedBy(CDC)
public class SectionChainStep implements ChildChainExecutable<SectionChainStepParameters> {
  public static final StepType STEP_TYPE = StepType.newBuilder().setType(OrchestrationStepTypes.SECTION_CHAIN).build();

  @Override
  public Class<SectionChainStepParameters> getStepParametersClass() {
    return SectionChainStepParameters.class;
  }

  @Override
  public ChildChainResponse executeFirstChild(
      Ambiance ambiance, SectionChainStepParameters sectionChainStepParameters, StepInputPackage inputPackage) {
    if (isEmpty(sectionChainStepParameters.getChildNodeIds())) {
      return ChildChainResponse.builder().suspend(true).build();
    }

    return ChildChainResponse.builder()
        .nextChildId(sectionChainStepParameters.getChildNodeIds().get(0))
        .passThroughData(SectionChainPassThroughData.builder().childIndex(0).build())
        .lastLink(sectionChainStepParameters.getChildNodeIds().size() == 1)
        .build();
  }

  @Override
  public ChildChainResponse executeNextChild(Ambiance ambiance, SectionChainStepParameters sectionChainStepParameters,
      StepInputPackage inputPackage, PassThroughData passThroughData, Map<String, ResponseData> responseDataMap) {
    SectionChainPassThroughData chainPassThroughData = (SectionChainPassThroughData) passThroughData;
    int nextChildIndex = chainPassThroughData.getChildIndex() + 1;
    String previousChildId = responseDataMap.keySet().iterator().next();
    boolean lastLink = nextChildIndex + 1 == sectionChainStepParameters.getChildNodeIds().size();
    chainPassThroughData.setChildIndex(nextChildIndex);
    return ChildChainResponse.builder()
        .passThroughData(chainPassThroughData)
        .nextChildId(sectionChainStepParameters.getChildNodeIds().get(nextChildIndex))
        .lastLink(lastLink)
        .previousChildId(previousChildId)
        .build();
  }

  @Override
  public StepResponse finalizeExecution(Ambiance ambiance, SectionChainStepParameters sectionChainStepParameters,
      PassThroughData passThroughData, Map<String, ResponseData> responseDataMap) {
    StepResponseBuilder responseBuilder = StepResponse.builder().status(Status.SUCCEEDED);
    for (ResponseData responseData : responseDataMap.values()) {
      Status executionStatus = ((StepResponseNotifyData) responseData).getStatus();
      if (executionStatus != Status.SUCCEEDED) {
        responseBuilder.status(executionStatus);
      }
    }
    return responseBuilder.build();
  }
}
