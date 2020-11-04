package io.harness.integrationstage;

import static io.harness.rule.OwnerRule.SHUBHAM;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.beans.executionargs.CIExecutionArgs;
import io.harness.category.element.UnitTests;
import io.harness.executionplan.CIExecutionPlanTestHelper;
import io.harness.executionplan.CIExecutionTest;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.Map;

public class BuildEnvironmentUtilsTest extends CIExecutionTest {
  @Inject CIExecutionPlanTestHelper ciExecutionPlanTestHelper;

  @Test
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void getPRBuildEnvironmentVariables() {
    CIExecutionArgs ciExecutionArgs = ciExecutionPlanTestHelper.getPRCIExecutionArgs();
    Map<String, String> actual = BuildEnvironmentUtils.getBuildEnvironmentVariables(ciExecutionArgs);
    Map<String, String> expected = ciExecutionPlanTestHelper.getPRCIExecutionArgsEnvVars();
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void getBranchBuildEnvironmentVariables() {
    CIExecutionArgs ciExecutionArgs = ciExecutionPlanTestHelper.getBranchCIExecutionArgs();
    Map<String, String> actual = BuildEnvironmentUtils.getBuildEnvironmentVariables(ciExecutionArgs);
    Map<String, String> expected = ciExecutionPlanTestHelper.getBranchCIExecutionArgsEnvVars();
    assertThat(actual).isEqualTo(expected);
  }
}