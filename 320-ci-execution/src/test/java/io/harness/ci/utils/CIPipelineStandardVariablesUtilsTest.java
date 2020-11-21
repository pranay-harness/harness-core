package io.harness.ci.utils;

import static io.harness.rule.OwnerRule.ALEKSANDAR;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.beans.executionargs.CIExecutionArgs;
import io.harness.category.element.UnitTests;
import io.harness.ci.beans.entities.BuildNumberDetails;
import io.harness.ci.stdvars.BuildStandardVariables;
import io.harness.executionplan.CIExecutionTest;
import io.harness.rule.Owner;

import org.junit.Test;
import org.junit.experimental.categories.Category;

public class CIPipelineStandardVariablesUtilsTest extends CIExecutionTest {
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
