package io.harness.cvng.statemachine.beans;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

import java.time.Instant;

@Data
@FieldNameConstants(innerTypeName = "AnalysisInputKeys")
@Builder
public class AnalysisInput {
  private Instant startTime;
  private Instant endTime;
  @Deprecated
  /**
   * Use verificationTaskId instead
   */
  private String cvConfigId;
  private String verificationTaskId;
}
