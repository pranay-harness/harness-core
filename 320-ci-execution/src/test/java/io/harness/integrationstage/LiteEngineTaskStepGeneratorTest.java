package io.harness.integrationstage;

import static io.harness.rule.OwnerRule.ALEKSANDAR;

import io.harness.beans.stages.IntegrationStage;
import io.harness.category.element.UnitTests;
import io.harness.ci.integrationstage.LiteEngineTaskStepGenerator;
import io.harness.executionplan.CIExecutionPlanTestHelper;
import io.harness.executionplan.CIExecutionTest;
import io.harness.rule.Owner;
import io.harness.yaml.core.ExecutionElement;

import com.google.inject.Inject;
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
    ExecutionElement executionElement = ciExecutionPlanTestHelper.getExpectedExecutionElement(false);
    IntegrationStage integrationStage = ciExecutionPlanTestHelper.getIntegrationStage();
    String buildNumber = "buildnumber22850";
    Integer liteEngineCounter = 1;
    boolean usePVC = true;
    String accountId = "accountId";

    //    CIExecutionArgs ciExecutionArgs = ciExecutionPlanTestHelper.getCIExecutionArgs();
    //    LiteEngineTaskStepInfo actual = liteEngineTaskStepGenerator.createLiteEngineTaskStepInfo(executionElement,
    //        ciExecutionPlanTestHelper.getCICodebase(), integrationStage, ciExecutionArgs, buildNumber,
    //        liteEngineCounter, usePVC, accountId);
    //    ((K8BuildJobEnvInfo) actual.getBuildJobEnvInfo())
    //        .getPodsSetupInfo()
    //        .getPodSetupInfoList()
    //        .forEach(podSetupInfo -> podSetupInfo.setName(""));
    //    ((K8BuildJobEnvInfo) actual.getBuildJobEnvInfo())
    //        .getPodsSetupInfo()
    //        .getPodSetupInfoList()
    //        .forEach(podSetupInfo -> podSetupInfo.getPvcParamsList().get(0).setClaimName(""));
    //
    //    LiteEngineTaskStepInfo expected = ciExecutionPlanTestHelper.getExpectedLiteEngineTaskInfoOnFirstPod();
    //    ((K8BuildJobEnvInfo) expected.getBuildJobEnvInfo())
    //        .getPodsSetupInfo()
    //        .getPodSetupInfoList()
    //        .forEach(podSetupInfo -> podSetupInfo.setName(""));
    //    ((K8BuildJobEnvInfo) expected.getBuildJobEnvInfo())
    //        .getPodsSetupInfo()
    //        .getPodSetupInfoList()
    //        .forEach(podSetupInfo -> podSetupInfo.getPvcParamsList().get(0).setClaimName(""));
    //
    //    assertThat(actual).isEqualTo(expected);
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
    //
    //    CIExecutionArgs ciExecutionArgs = ciExecutionPlanTestHelper.getCIExecutionArgs();
    //    LiteEngineTaskStepInfo actual = liteEngineTaskStepGenerator.createLiteEngineTaskStepInfo(
    //        executionElement, null, integrationStage, ciExecutionArgs, buildNumber, liteEngineCounter, usePVC,
    //        accountId);
    //    ((K8BuildJobEnvInfo) actual.getBuildJobEnvInfo())
    //        .getPodsSetupInfo()
    //        .getPodSetupInfoList()
    //        .forEach(podSetupInfo -> podSetupInfo.setName(""));
    //
    //    LiteEngineTaskStepInfo expected = ciExecutionPlanTestHelper.getExpectedLiteEngineTaskInfoOnOtherPods();
    //    ((K8BuildJobEnvInfo) expected.getBuildJobEnvInfo())
    //        .getPodsSetupInfo()
    //        .getPodSetupInfoList()
    //        .forEach(podSetupInfo -> podSetupInfo.setName(""));
    //
    //    assertThat(actual).isEqualTo(expected);
  }
}
