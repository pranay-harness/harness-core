package io.harness.adviser.impl.success;

import com.google.common.collect.ImmutableSet;

import io.harness.adviser.Advise;
import io.harness.adviser.Adviser;
import io.harness.adviser.AdviserType;
import io.harness.adviser.AdvisingEvent;
import io.harness.state.execution.status.NodeExecutionStatus;
import lombok.Builder;
import lombok.Value;

import java.util.Set;

@Value
@Builder
public class OnSuccessAdviser implements Adviser {
  private static final Set<NodeExecutionStatus> ADVISING_STATUSES =
      ImmutableSet.of(NodeExecutionStatus.SUCCEEDED, NodeExecutionStatus.SKIPPED);

  AdviserType type = AdviserType.builder().type(AdviserType.ON_SUCCESS).build();

  OnSuccessAdviserParameters onSuccessAdviserParameters;

  @Override
  public Advise onAdviseEvent(AdvisingEvent adviseEvent) {
    return OnSuccessAdvise.builder().nextNodeId(onSuccessAdviserParameters.getNextNodeId()).build();
  }

  @Override
  public boolean canAdvise(NodeExecutionStatus status) {
    return ADVISING_STATUSES.contains(status);
  }
}
