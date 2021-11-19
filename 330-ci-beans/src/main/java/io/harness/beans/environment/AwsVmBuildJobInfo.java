package io.harness.beans.environment;

import lombok.Builder;
import lombok.Data;
import lombok.Value;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.data.annotation.TypeAlias;

/**
 * Stores AWS specific data to setup VM for running CI job
 */

@Data
@Value
@Builder
@TypeAlias("awsVmBuildJobInfo")
public class AwsVmBuildJobInfo implements BuildJobEnvInfo {
  @NotEmpty private String workDir;

  @Override
  public Type getType() {
    return Type.AWS_VM;
  }
}