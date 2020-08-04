package io.harness.integrationstage;

import static io.harness.rule.OwnerRule.ALEKSANDAR;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.beans.environment.K8BuildJobEnvInfo;
import io.harness.beans.steps.stepinfo.LiteEngineTaskStepInfo;
import io.harness.category.element.UnitTests;
import io.harness.executionplan.CIExecutionPlanTestHelper;
import io.harness.executionplan.CIExecutionTest;
import io.harness.rule.Owner;
import io.harness.yaml.core.StepElement;
import io.harness.yaml.core.auxiliary.intfc.ExecutionWrapper;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.List;

public class CILiteEngineStepGroupUtilsTest extends CIExecutionTest {
  @Inject CILiteEngineStepGroupUtils ciLiteEngineStepGroupUtils;
  @Inject CIExecutionPlanTestHelper ciExecutionPlanTestHelper;

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void createExecutionWrapperWithLiteEngineSteps() {
    List<ExecutionWrapper> executionWrapperWithLiteEngineSteps =
        ciLiteEngineStepGroupUtils.createExecutionWrapperWithLiteEngineSteps(
            ciExecutionPlanTestHelper.getIntegrationStage(), "master", "testGitConnector");

    assertThat(executionWrapperWithLiteEngineSteps.get(0)).isInstanceOf(StepElement.class);
    StepElement stepElement = (StepElement) executionWrapperWithLiteEngineSteps.get(0);
    LiteEngineTaskStepInfo liteEngineTaskStepInfo = (LiteEngineTaskStepInfo) stepElement.getStepSpecType();
    ((K8BuildJobEnvInfo) liteEngineTaskStepInfo.getBuildJobEnvInfo())
        .getPodsSetupInfo()
        .getPodSetupInfoList()
        .get(0)
        .getPvcParams()
        .setClaimName("");
    ((K8BuildJobEnvInfo) liteEngineTaskStepInfo.getBuildJobEnvInfo())
        .getPodsSetupInfo()
        .getPodSetupInfoList()
        .get(0)
        .setName("");

    assertThat(executionWrapperWithLiteEngineSteps).isEqualTo(ciExecutionPlanTestHelper.getExpectedExecutionWrappers());
  }
}