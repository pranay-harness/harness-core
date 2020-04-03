package io.harness.facilitate.modes.child;

import io.harness.delegate.beans.ResponseData;
import io.harness.facilitate.PassThroughData;
import io.harness.state.io.StateExecutionPackage;
import io.harness.state.io.StateResponse;
import io.harness.state.io.ambiance.Ambiance;

import java.util.Map;

public interface ChildExecutable {
  ChildExecutableResponse obtainChild(
      StateExecutionPackage stateExecutionPackage, PassThroughData childPassThroughData);

  StateResponse handleAsyncResponse(Ambiance ambiance, Map<String, ResponseData> responseDataMap);
}
