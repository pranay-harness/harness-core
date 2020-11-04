package io.harness.integrationstage;

import static io.harness.rule.OwnerRule.ALEKSANDAR;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.beans.environment.K8BuildJobEnvInfo;
import io.harness.beans.executionargs.CIExecutionArgs;
import io.harness.beans.stages.IntegrationStage;
import io.harness.beans.steps.stepinfo.LiteEngineTaskStepInfo;
import io.harness.category.element.UnitTests;
import io.harness.executionplan.CIExecutionPlanTestHelper;
import io.harness.executionplan.CIExecutionTest;
import io.harness.rule.Owner;
import io.harness.yaml.core.ExecutionElement;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class LiteEngineTaskStepGeneratorTest extends CIExecutionTest {
  @Inject LiteEngineTaskStepGenerator liteEngineTaskStepGenerator;
  @Inject CIExecutionPlanTestHelper ciExecutionPlanTestHelper;

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void shouldCreateLiteEngineTaskStepInfoFirstPod() {
    // input
    ExecutionElement executionElement = ciExecutionPlanTestHelper.getExecutionElement();
    String branchName = "master";
    String gitConnectorIdentifier = "testGitConnector";
    IntegrationStage integrationStage = ciExecutionPlanTestHelper.getIntegrationStage();
    String buildNumber = "buildnumber22850";
    Integer liteEngineCounter = 1;
    boolean usePVC = true;
    String accountId = "accountId";

    CIExecutionArgs ciExecutionArgs = ciExecutionPlanTestHelper.getCIExecutionArgs();
    LiteEngineTaskStepInfo actual =
        liteEngineTaskStepGenerator.createLiteEngineTaskStepInfo(executionElement, branchName, gitConnectorIdentifier,
            integrationStage, ciExecutionArgs, buildNumber, liteEngineCounter, usePVC, accountId);
    ((K8BuildJobEnvInfo) actual.getBuildJobEnvInfo())
        .getPodsSetupInfo()
        .getPodSetupInfoList()
        .forEach(podSetupInfo -> podSetupInfo.setName(""));
    ((K8BuildJobEnvInfo) actual.getBuildJobEnvInfo())
        .getPodsSetupInfo()
        .getPodSetupInfoList()
        .forEach(podSetupInfo -> podSetupInfo.getPvcParams().setClaimName(""));

    LiteEngineTaskStepInfo expected = ciExecutionPlanTestHelper.getExpectedLiteEngineTaskInfoOnFirstPod();
    ((K8BuildJobEnvInfo) expected.getBuildJobEnvInfo())
        .getPodsSetupInfo()
        .getPodSetupInfoList()
        .forEach(podSetupInfo -> podSetupInfo.setName(""));
    ((K8BuildJobEnvInfo) expected.getBuildJobEnvInfo())
        .getPodsSetupInfo()
        .getPodSetupInfoList()
        .forEach(podSetupInfo -> podSetupInfo.getPvcParams().setClaimName(""));

    assertThat(actual).isEqualTo(expected);
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void shouldCreateLiteEngineTaskStepInfoOtherPod() {
    // input
    ExecutionElement executionElement = ciExecutionPlanTestHelper.getExecutionElement();
    String branchName = "master";
    String gitConnectorIdentifier = "testGitConnector";
    IntegrationStage integrationStage = ciExecutionPlanTestHelper.getIntegrationStage();
    String buildNumber = "buildnumber22850";
    Integer liteEngineCounter = 2;
    boolean usePVC = true;
    String accountId = "accountId";

    CIExecutionArgs ciExecutionArgs = ciExecutionPlanTestHelper.getCIExecutionArgs();
    LiteEngineTaskStepInfo actual =
        liteEngineTaskStepGenerator.createLiteEngineTaskStepInfo(executionElement, branchName, gitConnectorIdentifier,
            integrationStage, ciExecutionArgs, buildNumber, liteEngineCounter, usePVC, accountId);
    ((K8BuildJobEnvInfo) actual.getBuildJobEnvInfo())
        .getPodsSetupInfo()
        .getPodSetupInfoList()
        .forEach(podSetupInfo -> podSetupInfo.setName(""));

    LiteEngineTaskStepInfo expected = ciExecutionPlanTestHelper.getExpectedLiteEngineTaskInfoOnOtherPods();
    ((K8BuildJobEnvInfo) expected.getBuildJobEnvInfo())
        .getPodsSetupInfo()
        .getPodSetupInfoList()
        .forEach(podSetupInfo -> podSetupInfo.setName(""));

    assertThat(actual).isEqualTo(expected);
  }
}