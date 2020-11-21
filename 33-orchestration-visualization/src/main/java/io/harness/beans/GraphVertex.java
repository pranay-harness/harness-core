package io.harness.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.ambiance.Ambiance;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.Outcome;
import io.harness.interrupts.InterruptEffect;
import io.harness.pms.execution.ExecutionMode;
import io.harness.pms.execution.Status;
import io.harness.pms.steps.SkipType;
import io.harness.state.io.FailureInfo;
import io.harness.state.io.StepParameters;

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
  private StepParameters stepParameters;
  private ExecutionMode mode;

  private List<Map<String, String>> executableResponsesMetadata;
  private List<InterruptEffect> interruptHistories;
  private List<Outcome> outcomes;
  private List<String> retryIds;

  // skip
  private SkipType skipType;
}
