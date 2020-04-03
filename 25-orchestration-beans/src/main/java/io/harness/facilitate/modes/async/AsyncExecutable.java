package io.harness.facilitate.modes.async;

import io.harness.annotations.Redesign;
import io.harness.delegate.beans.ResponseData;
import io.harness.state.io.StateExecutionPackage;
import io.harness.state.io.StateResponse;
import io.harness.state.io.ambiance.Ambiance;

import java.util.Map;

@Redesign
public interface AsyncExecutable {
  AsyncExecutableResponse executeAsync(StateExecutionPackage stateExecutionPackage);
  StateResponse handleAsyncResponse(Ambiance ambiance, Map<String, ResponseData> responseDataMap);
}
