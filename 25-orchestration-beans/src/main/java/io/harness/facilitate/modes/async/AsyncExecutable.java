package io.harness.facilitate.modes.async;

import io.harness.ambiance.Ambiance;
import io.harness.annotations.Redesign;
import io.harness.delegate.beans.ResponseData;
import io.harness.state.io.StateParameters;
import io.harness.state.io.StateResponse;
import io.harness.state.io.StateTransput;

import java.util.List;
import java.util.Map;

@Redesign
public interface AsyncExecutable {
  AsyncExecutableResponse executeAsync(Ambiance ambiance, StateParameters parameters, List<StateTransput> inputs);

  StateResponse handleAsyncResponse(
      Ambiance ambiance, StateParameters parameters, Map<String, ResponseData> responseDataMap);
}
