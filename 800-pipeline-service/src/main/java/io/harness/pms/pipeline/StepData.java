package io.harness.pms.pipeline;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StepData {
  String name;
  String type;
}