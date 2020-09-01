package io.harness.cdng.pipeline;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StepData {
  NGStepType type;
  String name;
}
