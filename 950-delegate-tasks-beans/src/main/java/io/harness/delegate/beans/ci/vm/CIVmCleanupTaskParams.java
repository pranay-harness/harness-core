package io.harness.delegate.beans.ci.vm;

import io.harness.delegate.beans.ci.CICleanupTaskParams;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@JsonIgnoreProperties(ignoreUnknown = true)
public class CIVmCleanupTaskParams implements CICleanupTaskParams {
  @NotNull private String stageRuntimeId;
  @Builder.Default private static final CICleanupTaskParams.Type type = CICleanupTaskParams.Type.VM;

  @Override
  public CICleanupTaskParams.Type getType() {
    return type;
  }
}
