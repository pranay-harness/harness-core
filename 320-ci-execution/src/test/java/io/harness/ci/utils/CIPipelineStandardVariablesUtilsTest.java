/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ci.utils;

import static io.harness.rule.OwnerRule.ALEKSANDAR;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.beans.executionargs.CIExecutionArgs;
import io.harness.category.element.UnitTests;
import io.harness.ci.beans.entities.BuildNumberDetails;
import io.harness.ci.stdvars.BuildStandardVariables;
import io.harness.executionplan.CIExecutionTestBase;
import io.harness.rule.Owner;

import org.junit.Test;
import org.junit.experimental.categories.Category;

public class CIPipelineStandardVariablesUtilsTest extends CIExecutionTestBase {
  public static final long BUILD_NUMBER = 98765L;

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void shouldFetchBuildStandardVariables() {
    BuildNumberDetails buildNumberDetails = BuildNumberDetails.builder().buildNumber(BUILD_NUMBER).build();
    CIExecutionArgs ciExecutionArgs = CIExecutionArgs.builder().buildNumberDetails(buildNumberDetails).build();
    BuildStandardVariables buildStandardVariables =
        CIPipelineStandardVariablesUtils.fetchBuildStandardVariables(ciExecutionArgs);
    assertThat(buildStandardVariables).isEqualTo(BuildStandardVariables.builder().number(BUILD_NUMBER).build());
  }
}
