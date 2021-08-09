package io.harness.cvng.statemachine.beans;

import io.harness.cvng.core.beans.TimeRange;

import java.time.Instant;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

@Data
@FieldNameConstants(innerTypeName = "AnalysisInputKeys")
@Builder
public class AnalysisInput {
  private Instant startTime;
  private Instant endTime;
  private String verificationTaskId;

  public TimeRange getTimeRange() {
    return TimeRange.builder().startTime(startTime).endTime(endTime).build();
  }
}
