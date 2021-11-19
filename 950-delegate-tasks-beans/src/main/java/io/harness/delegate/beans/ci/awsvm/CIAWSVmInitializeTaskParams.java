package io.harness.delegate.beans.ci.awsvm;

import io.harness.delegate.beans.ci.CIInitializeTaskParams;
import io.harness.delegate.beans.connector.ConnectorTaskParams;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@JsonIgnoreProperties(ignoreUnknown = true)
public class CIAWSVmInitializeTaskParams extends ConnectorTaskParams implements CIInitializeTaskParams {
  @NotNull private String poolId;
  @NotNull private String stageRuntimeId;
  @Builder.Default private static final Type type = Type.AWS_VM;

  @Override
  public Type getType() {
    return type;
  }
}
