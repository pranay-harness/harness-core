package io.harness.pms.pipeline;

import io.harness.pms.contracts.steps.StepInfo;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StepPalleteInfo {
  String moduleName;
  List<StepInfo> stepTypes;
}
