package io.harness.ng.core.dashboard;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@OwnedBy(HarnessTeam.CDC)
@Value
@Builder
public class DashboardExecutionStatusInfo {
  private List<ExecutionStatusInfo> failure;
  private List<ExecutionStatusInfo> pending;
  private List<ExecutionStatusInfo> active;
}
