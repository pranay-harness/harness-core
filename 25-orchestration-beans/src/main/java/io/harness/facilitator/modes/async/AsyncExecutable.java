package io.harness.facilitator.modes.async;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.ambiance.Ambiance;
import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.ResponseData;
import io.harness.state.io.StateParameters;
import io.harness.state.io.StepResponse;
import io.harness.state.io.StepTransput;

import java.util.List;
import java.util.Map;

@OwnedBy(CDC)
@Redesign
public interface AsyncExecutable {
  AsyncExecutableResponse executeAsync(Ambiance ambiance, StateParameters parameters, List<StepTransput> inputs);

  StepResponse handleAsyncResponse(
      Ambiance ambiance, StateParameters parameters, Map<String, ResponseData> responseDataMap);
}
