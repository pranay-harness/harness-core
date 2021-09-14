/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ci.utils;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.executionargs.CIExecutionArgs;
import io.harness.ci.stdvars.BuildStandardVariables;

import com.google.inject.Singleton;
import lombok.Builder;

@Singleton
@Builder
@OwnedBy(HarnessTeam.CI)
public class CIPipelineStandardVariablesUtils {
  public static BuildStandardVariables fetchBuildStandardVariables(CIExecutionArgs ciExecutionArgs) {
    return BuildStandardVariables.builder().number(ciExecutionArgs.getBuildNumberDetails().getBuildNumber()).build();
  }
}
