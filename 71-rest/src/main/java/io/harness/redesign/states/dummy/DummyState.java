package io.harness.redesign.states.dummy;

import io.harness.annotations.Redesign;
import io.harness.facilitate.PassThroughData;
import io.harness.facilitate.modes.sync.SyncExecutable;
import io.harness.state.State;
import io.harness.state.execution.status.NodeExecutionStatus;
import io.harness.state.io.StateParameters;
import io.harness.state.io.StateResponse;
import io.harness.state.io.StateTransput;
import io.harness.state.io.ambiance.Ambiance;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@Redesign
public class DummyState implements State, SyncExecutable {
  @Override
  public StateResponse executeSync(
      Ambiance ambiance, StateParameters parameters, List<StateTransput> inputs, PassThroughData passThroughData) {
    logger.info("Dummy State getting executed");
    return StateResponse.builder().status(NodeExecutionStatus.SUCCEEDED).build();
  }

  @Override
  public String getStateType() {
    return "DUMMY";
  }
}
