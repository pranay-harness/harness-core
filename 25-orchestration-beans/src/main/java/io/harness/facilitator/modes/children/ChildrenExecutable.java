package io.harness.facilitator.modes.children;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.ambiance.Ambiance;
import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.ResponseData;
import io.harness.state.io.StepParameters;
import io.harness.state.io.StepResponse;
import io.harness.state.io.StepTransput;

import java.util.List;
import java.util.Map;

@OwnedBy(CDC)
@Redesign
public interface ChildrenExecutable {
  ChildrenExecutableResponse obtainChildren(
      Ambiance ambiance, StepParameters stepParameters, List<StepTransput> inputs);

  StepResponse handleChildrenResponse(
      Ambiance ambiance, StepParameters stepParameters, Map<String, ResponseData> responseDataMap);
}
