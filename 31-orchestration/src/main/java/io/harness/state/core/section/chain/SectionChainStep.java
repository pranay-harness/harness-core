package io.harness.state.core.section.chain;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.ambiance.Ambiance;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.ResponseData;
import io.harness.facilitator.PassThroughData;
import io.harness.facilitator.modes.chain.child.ChildChainExecutable;
import io.harness.facilitator.modes.chain.child.ChildChainResponse;
import io.harness.state.Step;
import io.harness.state.StepType;
import io.harness.state.io.StepInputPackage;
import io.harness.state.io.StepParameters;
import io.harness.state.io.StepResponse;
import io.harness.state.io.StepResponseNotifyData;

import java.util.Map;

@OwnedBy(CDC)
public class SectionChainStep implements Step, ChildChainExecutable {
  public static final StepType STEP_TYPE = StepType.builder().type("SECTION_CHAIN").build();

  @Override
  public ChildChainResponse executeFirstChild(
      Ambiance ambiance, StepParameters stepParameters, StepInputPackage inputPackage) {
    SectionChainStepParameters parameters = (SectionChainStepParameters) stepParameters;
    return ChildChainResponse.builder()
        .nextChildId(parameters.getChildNodeIds().get(0))
        .passThroughData(SectionChainPassThroughData.builder().childIndex(0).build())
        .lastLink(parameters.getChildNodeIds().size() == 1)
        .build();
  }

  @Override
  public ChildChainResponse executeNextChild(Ambiance ambiance, StepParameters stepParameters,
      StepInputPackage inputPackage, PassThroughData passThroughData, Map<String, ResponseData> responseDataMap) {
    SectionChainStepParameters parameters = (SectionChainStepParameters) stepParameters;
    SectionChainPassThroughData chainPassThroughData = (SectionChainPassThroughData) passThroughData;
    int nextChildIndex = chainPassThroughData.getChildIndex() + 1;
    String previousChildId = responseDataMap.keySet().iterator().next();
    boolean lastLink = nextChildIndex + 1 == parameters.getChildNodeIds().size();
    chainPassThroughData.setChildIndex(nextChildIndex);
    return ChildChainResponse.builder()
        .passThroughData(chainPassThroughData)
        .nextChildId(parameters.getChildNodeIds().get(nextChildIndex))
        .lastLink(lastLink)
        .previousChildId(previousChildId)
        .build();
  }

  @Override
  public StepResponse finalizeExecution(Ambiance ambiance, StepParameters stepParameters,
      PassThroughData passThroughData, Map<String, ResponseData> responseDataMap) {
    StepResponseNotifyData notifyData = (StepResponseNotifyData) responseDataMap.values().iterator().next();
    return StepResponse.builder().status(notifyData.getStatus()).failureInfo(notifyData.getFailureInfo()).build();
  }
}
