/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.integrationstage;

import static io.harness.rule.OwnerRule.SHUBHAM;

import io.harness.beans.executionargs.CIExecutionArgs;
import io.harness.category.element.UnitTests;
import io.harness.executionplan.CIExecutionPlanTestHelper;
import io.harness.executionplan.CIExecutionTestBase;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class BuildEnvironmentUtilsTest extends CIExecutionTestBase {
  @Inject CIExecutionPlanTestHelper ciExecutionPlanTestHelper;

  @Test
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void getPRBuildEnvironmentVariables() {
    CIExecutionArgs ciExecutionArgs = ciExecutionPlanTestHelper.getPRCIExecutionArgs();
    //    Map<String, String> actual = BuildEnvironmentUtils.getBuildEnvironmentVariables(ciExecutionArgs);
    //    Map<String, String> expected = ciExecutionPlanTestHelper.getPRCIExecutionArgsEnvVars();
    //    assertThat(actual).isEqualTo(expected);
  }

  @Test
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void getBranchBuildEnvironmentVariables() {
    //    CIExecutionArgs ciExecutionArgs = ciExecutionPlanTestHelper.getBranchCIExecutionArgs();
    //    Map<String, String> actual = BuildEnvironmentUtils.getBuildEnvironmentVariables(ciExecutionArgs);
    //    Map<String, String> expected = ciExecutionPlanTestHelper.getBranchCIExecutionArgsEnvVars();
    //    assertThat(actual).isEqualTo(expected);
  }
}
