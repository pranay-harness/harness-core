package io.harness.statemachine.entity;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class AnalysisInput {
  Instant startTime;
  Instant endTime;
  private String cvConfigId;
}
