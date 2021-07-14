package io.harness.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.dto.GraphDelegateSelectionLogParams;
import io.harness.interrupts.InterruptEffect;
import io.harness.logging.UnitProgress;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.ExecutableResponse;
import io.harness.pms.contracts.execution.ExecutionMode;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.failure.FailureInfo;
import io.harness.pms.contracts.execution.run.NodeRunInfo;
import io.harness.pms.contracts.execution.skip.SkipInfo;
import io.harness.pms.contracts.steps.SkipType;
import io.harness.pms.data.OrchestrationMap;
import io.harness.tasks.ProgressData;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.Serializable;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@OwnedBy(CDC)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GraphVertex implements Serializable {
  private String uuid;
  private Ambiance ambiance;
  private String planNodeId;
  private String identifier;
  private String name;
  private Long startTs;
  private Long endTs;
  private Duration initialWaitDuration;
  private Long lastUpdatedAt;
  private String stepType;
  private Status status;
  private FailureInfo failureInfo;
  private OrchestrationMap stepParameters;
  private ExecutionMode mode;

  private List<ExecutableResponse> executableResponses;
  private List<GraphDelegateSelectionLogParams> graphDelegateSelectionLogParams;
  private List<InterruptEffect> interruptHistories;
  private Map<String, OrchestrationMap> outcomeDocuments;
  private List<String> retryIds;

  private Map<String, List<ProgressData>> progressDataMap;

  private SkipInfo skipInfo;
  private NodeRunInfo nodeRunInfo;
  // skip
  private SkipType skipType;

  private List<UnitProgress> unitProgresses;
  private OrchestrationMap progressData;

  // UI
  @Builder.Default RepresentationStrategy representationStrategy = RepresentationStrategy.CAMELCASE;
}
