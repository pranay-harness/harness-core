package io.harness.pms.sdk.core.steps.io;

import io.harness.pms.serializer.json.JsonOrchestrationIgnore;
import io.harness.pms.yaml.ParameterField;

import com.fasterxml.jackson.annotation.JsonIgnore;

public interface WithRollbackInfo {
  @JsonIgnore
  @JsonOrchestrationIgnore
  StepParameters getStepParametersWithRollbackInfo(RollbackInfo rollbackInfo, ParameterField<String> timeout);
}
