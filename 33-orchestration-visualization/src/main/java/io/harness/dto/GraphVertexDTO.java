package io.harness.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.harness.data.Outcome;
import io.harness.interrupts.InterruptEffect;
import io.harness.pms.execution.ExecutionMode;
import io.harness.pms.execution.Status;
import io.harness.skip.SkipType;
import io.harness.state.io.FailureInfo;
import io.harness.state.io.StepParameters;
import lombok.Builder;
import lombok.Value;

import java.time.Duration;
import java.util.List;
import java.util.Map;

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
  StepParameters stepParameters;
  ExecutionMode mode;

  List<Map<String, String>> executableResponsesMetadata;
  List<InterruptEffect> interruptHistories;
  List<Outcome> outcomes;
  List<String> retryIds;

  // skip
  SkipType skipType;
}
