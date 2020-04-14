package io.harness.facilitate.modes.children;

import io.harness.annotations.Redesign;
import io.harness.delegate.beans.ResponseData;
import io.harness.state.io.StateParameters;
import io.harness.state.io.StateResponse;
import io.harness.state.io.StateTransput;
import io.harness.state.io.ambiance.Ambiance;

import java.util.List;
import java.util.Map;

@Redesign
public interface ChildrenExecutable {
  ChildrenExecutableResponse obtainChildren(Ambiance ambiance, StateParameters parameters, List<StateTransput> inputs);

  StateResponse handleAsyncResponse(
      Ambiance ambiance, StateParameters stateParameters, Map<String, ResponseData> responseDataMap);
}
