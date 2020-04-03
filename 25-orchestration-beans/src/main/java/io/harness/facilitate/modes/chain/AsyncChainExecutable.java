package io.harness.facilitate.modes.chain;

import io.harness.annotations.Redesign;
import io.harness.delegate.beans.ResponseData;
import io.harness.facilitate.modes.async.AsyncExecutableResponse;
import io.harness.state.io.StateExecutionPackage;
import io.harness.state.io.StateResponse;
import io.harness.state.io.ambiance.Ambiance;

import java.util.Map;

@Redesign
public interface AsyncChainExecutable {
  AsyncExecutableResponse executeAsync(StateExecutionPackage stateExecutionPackage);

  AsyncChainResponse handleAsyncIntermediate(
      StateExecutionPackage stateExecutionPackage, Map<String, ResponseData> responseDataMap);

  StateResponse finalizeExecution(Ambiance ambiance, Map<String, ResponseData> responseDataMap);
}
