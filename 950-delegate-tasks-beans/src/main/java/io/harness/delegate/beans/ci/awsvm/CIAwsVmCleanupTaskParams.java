package io.harness.delegate.beans.ci.awsvm;

import io.harness.delegate.beans.ci.CICleanupTaskParams;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@JsonIgnoreProperties(ignoreUnknown = true)
public class CIAwsVmCleanupTaskParams implements CICleanupTaskParams {
  @NotNull private String stageRuntimeId;
  @Builder.Default private static final CICleanupTaskParams.Type type = CICleanupTaskParams.Type.AWS_VM;

  @Override
  public CICleanupTaskParams.Type getType() {
    return type;
  }
}
