package io.harness.facilitator.modes.chain.child;

import io.harness.ambiance.Ambiance;
import io.harness.delegate.beans.ResponseData;
import io.harness.facilitator.PassThroughData;
import io.harness.state.io.StepParameters;
import io.harness.state.io.StepResponse;
import io.harness.state.io.StepTransput;

import java.util.List;
import java.util.Map;

public interface ChildChainExecutable {
  ChildChainResponse executeFirstChild(Ambiance ambiance, StepParameters stepParameters, List<StepTransput> inputs);

  ChildChainResponse executeNextChild(Ambiance ambiance, StepParameters stepParameters, List<StepTransput> inputs,
      PassThroughData passThroughData, Map<String, ResponseData> responseDataMap);

  StepResponse finalizeExecution(
      Ambiance ambiance, StepParameters stepParameters, Map<String, ResponseData> responseDataMap);
}
