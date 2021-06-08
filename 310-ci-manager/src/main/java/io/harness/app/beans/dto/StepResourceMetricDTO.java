package io.harness.app.beans.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class StepResourceMetricDTO {
  int currentMemoryMib;
  int maxMemoryMib;
  int suggestedMemoryMib;
  boolean isMemoryOverProvisioned;

  int currentMilliCpu;
  int maxMilliCpu;
  int suggestedMilliCpu;
  boolean isCpuOverProvisioned;

  String stepId;
  String stageId;
}
