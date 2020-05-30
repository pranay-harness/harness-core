package io.harness.engine.executables;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.ambiance.Ambiance;
import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.ResponseData;
import io.harness.facilitator.PassThroughData;
import io.harness.state.Step;
import io.harness.state.io.StepParameters;
import io.harness.state.io.StepTransput;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Map;

@OwnedBy(CDC)
@Value
@Builder
@Redesign
public class InvokerPackage {
  Step step;
  Ambiance ambiance;
  StepParameters parameters;
  List<StepTransput> inputs;
  PassThroughData passThroughData;

  // TODO (prashant) -> Indicates that the ASYNC_TASK_CHAIN is starting. Do Not like this figure out a better way. In
  // fact think how this whole chain should work. Also is this required: States should do one unit of work
  boolean start;
  Map<String, ResponseData> responseDataMap;
}
