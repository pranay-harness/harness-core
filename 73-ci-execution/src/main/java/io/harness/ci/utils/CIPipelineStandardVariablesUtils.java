package io.harness.ci.utils;

import com.google.inject.Singleton;

import io.harness.beans.executionargs.CIExecutionArgs;
import io.harness.ci.stdvars.BuildStandardVariables;
import lombok.Builder;

@Singleton
@Builder
public class CIPipelineStandardVariablesUtils {
  public static BuildStandardVariables fetchBuildStandardVariables(CIExecutionArgs ciExecutionArgs) {
    return BuildStandardVariables.builder().number(ciExecutionArgs.getBuildNumber()).build();
  }
}
