package io.harness.beans.environment;

import io.harness.beans.executionargs.CIExecutionArgs;

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
@TypeAlias("vmBuildJobInfo")
public class VmBuildJobInfo implements BuildJobEnvInfo {
  @NotEmpty private String workDir;
  private CIExecutionArgs ciExecutionArgs;

  @Override
  public Type getType() {
    return Type.VM;
  }
}