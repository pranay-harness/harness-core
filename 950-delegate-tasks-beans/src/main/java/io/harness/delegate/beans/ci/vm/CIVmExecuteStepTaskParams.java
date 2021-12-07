package io.harness.delegate.beans.ci.vm;

import io.harness.delegate.beans.ci.CIExecuteStepTaskParams;
import io.harness.delegate.beans.ci.vm.steps.StepInfo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@JsonIgnoreProperties(ignoreUnknown = true)
public class CIVmExecuteStepTaskParams implements CIExecuteStepTaskParams {
  @NotNull private String ipAddress;
  @NotNull private String stepRuntimeId;
  @NotNull private String stepId;
  @NotNull private StepInfo stepInfo;
  @NotNull private String logKey;
  @NotNull private String workingDir;
  private int timeoutSecs;

  private String poolId;
  private String stageRuntimeId;

  @Builder.Default private static final Type type = Type.VM;

  @Override
  public Type getType() {
    return type;
  }
}
