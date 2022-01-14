package io.harness.beans;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.dto.FailureInfoDTO;
import io.harness.interrupts.InterruptEffect;
import io.harness.logging.UnitProgress;
import io.harness.pms.contracts.execution.ExecutableResponse;
import io.harness.pms.contracts.execution.run.NodeRunInfo;
import io.harness.pms.contracts.execution.skip.SkipInfo;
import io.harness.pms.data.OrchestrationMap;
import io.harness.pms.execution.ExecutionStatus;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(PIPELINE)
public class ExecutionNode {
  String uuid;
  String setupId;
  String name;
  String identifier;
  String baseFqn;
  Map<String, OrchestrationMap> outcomes;
  OrchestrationMap stepParameters;
  Long startTs;
  Long endTs;
  String stepType;
  ExecutionStatus status;
  FailureInfoDTO failureInfo;
  SkipInfo skipInfo;
  NodeRunInfo nodeRunInfo;
  List<ExecutableResponse> executableResponses;
  List<UnitProgress> unitProgresses;
  OrchestrationMap progressData;
  List<DelegateInfo> delegateInfoList;
  List<InterruptEffect> interruptHistories;
  Map<String, OrchestrationMap> stepDetails;
}
