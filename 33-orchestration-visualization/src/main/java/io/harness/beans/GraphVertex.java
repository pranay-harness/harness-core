package io.harness.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.Outcome;
import io.harness.execution.status.Status;
import io.harness.facilitator.modes.ExecutionMode;
import io.harness.interrupts.InterruptEffect;
import io.harness.skip.SkipType;
import io.harness.state.io.FailureInfo;
import io.harness.state.io.StepParameters;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Duration;
import java.util.List;

@OwnedBy(CDC)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GraphVertex implements Serializable {
  private String uuid;
  private String planNodeId;
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

  private List<InterruptEffect> interruptHistories;
  private List<Outcome> outcomes;
  private List<String> retryIds;

  // skip
  private SkipType skipType;
}
