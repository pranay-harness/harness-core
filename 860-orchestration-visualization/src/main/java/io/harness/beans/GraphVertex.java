package io.harness.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.interrupts.InterruptEffect;
import io.harness.pms.ambiance.Ambiance;
import io.harness.pms.execution.ExecutionMode;
import io.harness.pms.execution.Status;
import io.harness.pms.execution.failure.FailureInfo;
import io.harness.pms.sdk.core.data.Metadata;
import io.harness.pms.sdk.core.data.Outcome;
import io.harness.pms.steps.SkipType;
import io.harness.tasks.ProgressData;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.protobuf.ByteString;
import java.io.Serializable;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.sf.json.JSON;

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
  private Map<String, Object> stepParameters;
  private ExecutionMode mode;

  private List<Map<String, Metadata>> executableResponsesMetadata;
  private List<InterruptEffect> interruptHistories;
  private List<Outcome> outcomes;
  private List<String> retryIds;

  private Map<String, List<ProgressData>> progressDataMap;

  // skip
  private SkipType skipType;
}
