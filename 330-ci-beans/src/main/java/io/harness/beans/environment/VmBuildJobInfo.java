package io.harness.beans.environment;

import io.harness.beans.executionargs.CIExecutionArgs;
import io.harness.yaml.core.variables.NGVariable;

import java.util.ArrayList;
import java.util.List;
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
  private ArrayList<String> connectorRefs;
  private List<NGVariable> stageVars;

  @Override
  public Type getType() {
    return Type.VM;
  }
}