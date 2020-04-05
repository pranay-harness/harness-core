package io.harness.facilitate.modes.children;

import io.harness.delegate.beans.ResponseData;
import io.harness.state.io.StateExecutionPackage;
import io.harness.state.io.StateResponse;
import io.harness.state.io.ambiance.Ambiance;

import java.util.Map;

public interface ChildrenExecutable {
  ChildrenExecutableResponse obtainChildren(StateExecutionPackage stateExecutionPackage);

  StateResponse handleAsyncResponse(Ambiance ambiance, Map<String, ResponseData> responseDataMap);
}
