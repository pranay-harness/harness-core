package io.harness.facilitator.modes.chain;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.ambiance.Ambiance;
import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.ResponseData;
import io.harness.facilitator.modes.async.AsyncExecutableResponse;
import io.harness.state.io.StepParameters;
import io.harness.state.io.StepResponse;
import io.harness.state.io.StepTransput;

import java.util.List;
import java.util.Map;

@OwnedBy(CDC)
@Redesign
public interface AsyncChainExecutable {
  AsyncExecutableResponse executeChainAsync(
      Ambiance ambiance, StepParameters stepParameters, List<StepTransput> inputs);

  AsyncChainResponse handleAsyncIntermediate(Ambiance ambiance, StepParameters stepParameters,
      List<StepTransput> inputs, Map<String, ResponseData> responseDataMap);

  StepResponse finalizeExecution(
      Ambiance ambiance, StepParameters stepParameters, Map<String, ResponseData> responseDataMap);
}
