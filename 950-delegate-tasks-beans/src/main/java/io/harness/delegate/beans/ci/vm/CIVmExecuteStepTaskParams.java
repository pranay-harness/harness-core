package io.harness.delegate.beans.ci.vm;

import io.harness.delegate.beans.ci.CIExecuteStepTaskParams;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@JsonIgnoreProperties(ignoreUnknown = true)
public class CIVmExecuteStepTaskParams implements CIExecuteStepTaskParams {
  @NotNull private String poolId;
  @NotNull private String stageRuntimeId;

  @NotNull String stepId;
  String command;
  String image;
  @NotNull String logKey;
  @NotNull String accountId;
  @NotNull String logToken;
  @NotNull String logStreamUrl;

  @Builder.Default private static final Type type = Type.VM;

  @Override
  public Type getType() {
    return type;
  }
}
