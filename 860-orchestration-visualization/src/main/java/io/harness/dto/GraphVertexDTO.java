/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

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
import io.harness.pms.data.OrchestrationMap;
import io.harness.pms.data.PmsOutcome;
import io.harness.pms.data.stepdetails.PmsStepDetails;
import io.harness.pms.data.stepparameters.PmsStepParameters;
import io.harness.pms.utils.OrchestrationMapBackwardCompatibilityUtils;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Value;

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
  PmsStepParameters stepParameters;
  ExecutionMode mode;

  List<GraphDelegateSelectionLogParams> graphDelegateSelectionLogParams;
  List<ExecutableResponse> executableResponses;
  List<InterruptEffect> interruptHistories;
  Map<String, PmsOutcome> outcomes;
  List<String> retryIds;

  List<UnitProgress> unitProgresses;
  OrchestrationMap progressData;
  Map<String, PmsStepDetails> stepDetails;

  // skip
  SkipType skipType;

  // UI
  RepresentationStrategy representationStrategy = RepresentationStrategy.CAMELCASE;

  public Map<String, OrchestrationMap> getOrchestrationMapOutcomes() {
    return OrchestrationMapBackwardCompatibilityUtils.convertToOrchestrationMap(outcomes);
  }

  public Map<String, OrchestrationMap> getOrchestrationMapStepDetails() {
    return OrchestrationMapBackwardCompatibilityUtils.convertToOrchestrationMap(stepDetails);
  }
}
