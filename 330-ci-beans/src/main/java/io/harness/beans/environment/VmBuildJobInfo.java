package io.harness.beans.environment;

import io.harness.beans.executionargs.CIExecutionArgs;
import io.harness.yaml.core.variables.NGVariable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
  @NotEmpty String workDir;
  CIExecutionArgs ciExecutionArgs;
  ArrayList<String> connectorRefs;
  List<NGVariable> stageVars;
  Map<String, String> volToMountPath;

  @Override
  public Type getType() {
    return Type.VM;
  }
}