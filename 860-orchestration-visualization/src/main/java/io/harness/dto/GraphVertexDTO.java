package io.harness.dto;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.RepresentationStrategy;
import io.harness.interrupts.InterruptEffect;
import io.harness.logging.UnitProgress;
import io.harness.pms.contracts.execution.ExecutableResponse;
import io.harness.pms.contracts.execution.ExecutionMode;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.run.NodeRunInfo;
import io.harness.pms.contracts.execution.skip.SkipInfo;
import io.harness.pms.contracts.steps.SkipType;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Duration;
import java.util.List;
import lombok.Builder;
import lombok.Value;
import org.bson.Document;

@OwnedBy(HarnessTeam.PIPELINE)
@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GraphVertexDTO {
  String uuid;
  AmbianceDTO ambiance;
  String planNodeId;
  String identifier;
  String name;
  Long startTs;
  Long endTs;
  Duration initialWaitDuration;
  Long lastUpdatedAt;
  String stepType;
  Status status;
  FailureInfoDTO failureInfo;
  SkipInfo skipInfo;
  NodeRunInfo nodeRunInfo;
  Document stepParameters;
  ExecutionMode mode;

  List<GraphDelegateSelectionLogParams> graphDelegateSelectionLogParams;
  List<ExecutableResponse> executableResponses;
  List<InterruptEffect> interruptHistories;
  List<Document> outcomes;
  List<String> retryIds;

  List<UnitProgress> unitProgresses;
  Document progressData;

  // skip
  SkipType skipType;

  // UI
  RepresentationStrategy representationStrategy = RepresentationStrategy.CAMELCASE;
}
