package io.harness.dto;

import io.harness.interrupts.InterruptEffect;
import io.harness.pms.contracts.execution.ExecutionMode;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.failure.FailureInfo;
import io.harness.pms.contracts.steps.SkipType;
import io.harness.pms.sdk.core.data.Outcome;
import io.harness.tasks.ProgressData;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Value;

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
  FailureInfo failureInfo;
  Map<String, Object> stepParameters;
  ExecutionMode mode;

  List<Map<String, Object>> executableResponsesMetadata;
  List<InterruptEffect> interruptHistories;
  List<Outcome> outcomes;
  List<String> retryIds;

  Map<String, List<ProgressData>> progressDataMap;

  // skip
  SkipType skipType;
}
