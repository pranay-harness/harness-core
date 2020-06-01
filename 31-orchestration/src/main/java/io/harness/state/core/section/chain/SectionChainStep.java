package io.harness.state.core.section.chain;

import static io.harness.execution.status.Status.SUCCEEDED;

import io.harness.ambiance.Ambiance;
import io.harness.delegate.beans.ResponseData;
import io.harness.facilitator.PassThroughData;
import io.harness.facilitator.modes.chain.child.ChildChainExecutable;
import io.harness.facilitator.modes.chain.child.ChildChainResponse;
import io.harness.state.Step;
import io.harness.state.StepType;
import io.harness.state.io.StepParameters;
import io.harness.state.io.StepResponse;
import io.harness.state.io.StepTransput;

import java.util.List;
import java.util.Map;

public class SectionChainStep implements Step, ChildChainExecutable {
  public static final StepType STEP_TYPE = StepType.builder().type("SECTION_CHAIN").build();

  @Override
  public ChildChainResponse executeFirstChild(
      Ambiance ambiance, StepParameters stepParameters, List<StepTransput> inputs) {
    SectionChainStepParameters parameters = (SectionChainStepParameters) stepParameters;
    return ChildChainResponse.builder()
        .childNodeId(parameters.getChildNodeIds().get(0))
        .passThroughData(SectionChainPassThroughData.builder().childIndex(0).build())
        .chainEnd(false)
        .build();
  }

  @Override
  public ChildChainResponse executeNextChild(Ambiance ambiance, StepParameters stepParameters,
      List<StepTransput> inputs, PassThroughData passThroughData, Map<String, ResponseData> responseDataMap) {
    SectionChainStepParameters parameters = (SectionChainStepParameters) stepParameters;
    SectionChainPassThroughData chainPassThroughData = (SectionChainPassThroughData) passThroughData;
    int nextChildIndex = chainPassThroughData.getChildIndex() + 1;
    boolean chainEnd = nextChildIndex + 1 == parameters.getChildNodeIds().size();
    chainPassThroughData.setChildIndex(nextChildIndex);
    return ChildChainResponse.builder()
        .passThroughData(chainPassThroughData)
        .childNodeId(parameters.getChildNodeIds().get(nextChildIndex))
        .chainEnd(chainEnd)
        .build();
  }

  @Override
  public StepResponse finalizeExecution(
      Ambiance ambiance, StepParameters stepParameters, Map<String, ResponseData> responseDataMap) {
    return StepResponse.builder().status(SUCCEEDED).build();
  }
}
