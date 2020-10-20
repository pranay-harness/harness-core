

package io.harness.stateutils.buildstate;

import static io.harness.common.CIExecutionConstants.ACCESS_KEY_MINIO_VARIABLE;
import static io.harness.common.CIExecutionConstants.HARNESS_ACCOUNT_ID_VARIABLE;
import static io.harness.common.CIExecutionConstants.HARNESS_BUILD_ID_VARIABLE;
import static io.harness.common.CIExecutionConstants.HARNESS_ORG_ID_VARIABLE;
import static io.harness.common.CIExecutionConstants.HARNESS_PROJECT_ID_VARIABLE;
import static io.harness.common.CIExecutionConstants.HARNESS_STAGE_ID_VARIABLE;
import static io.harness.common.CIExecutionConstants.LOG_SERVICE_ENDPOINT_VARIABLE;
import static io.harness.common.CIExecutionConstants.LOG_SERVICE_ENDPOINT_VARIABLE_VALUE;
import static io.harness.common.CIExecutionConstants.SECRET_KEY_MINIO_VARIABLE;
import static io.harness.rule.OwnerRule.HARSH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.joor.Reflect.on;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static software.wings.common.CICommonPodConstants.MOUNT_PATH;
import static software.wings.common.CICommonPodConstants.STEP_EXEC;
import static software.wings.common.CICommonPodConstants.STEP_EXEC_WORKING_DIR;

import com.google.inject.Inject;

import io.harness.beans.environment.K8BuildJobEnvInfo;
import io.harness.beans.sweepingoutputs.K8PodDetails;
import io.harness.beans.sweepingoutputs.StepTaskDetails;
import io.harness.category.element.UnitTests;
import io.harness.ci.beans.entities.BuildNumber;
import io.harness.engine.outputs.ExecutionSweepingOutputService;
import io.harness.executionplan.CIExecutionPlanTestHelper;
import io.harness.executionplan.CIExecutionTest;
import io.harness.rule.Owner;
import io.harness.stateutils.buildstate.providers.InternalContainerParamsProvider;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import software.wings.beans.ci.pod.CIK8ContainerParams;
import software.wings.beans.ci.pod.CIK8PodParams;
import software.wings.beans.ci.pod.ContainerSecrets;
import software.wings.beans.ci.pod.EncryptedVariableWithType;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class K8BuildSetupUtilsTest extends CIExecutionTest {
  @Inject private BuildSetupUtils buildSetupUtils;
  @Inject private CIExecutionPlanTestHelper ciExecutionPlanTestHelper;
  @Inject private K8BuildSetupUtils k8BuildSetupUtils;

  @Mock ExecutionSweepingOutputService executionSweepingOutputResolver;

  @Before
  public void setUp() {
    on(k8BuildSetupUtils).set("executionSweepingOutputResolver", executionSweepingOutputResolver);
  }

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  public void shouldCreatePodParameters() throws IOException {
    when(executionSweepingOutputResolver.resolve(any(), any())).thenReturn(StepTaskDetails.builder().build());

    K8BuildJobEnvInfo.PodsSetupInfo podsSetupInfo = ciExecutionPlanTestHelper.getCIPodsSetupInfoOnFirstPod();

    String accountID = "account";
    String orgID = "org";
    String projectID = "project";
    Long buildID = 1L;
    String stageID = "stage";
    String namespace = "default";
    BuildNumber buildNumber = BuildNumber.builder()
                                  .accountIdentifier(accountID)
                                  .orgIdentifier(orgID)
                                  .projectIdentifier(projectID)
                                  .buildNumber(buildID)
                                  .build();
    K8PodDetails k8PodDetails =
        K8PodDetails.builder().namespace(namespace).buildNumber(buildNumber).stageID(stageID).build();

    CIK8PodParams<CIK8ContainerParams> podParams =
        k8BuildSetupUtils.getPodParams(podsSetupInfo.getPodSetupInfoList().get(0), k8PodDetails,
            ciExecutionPlanTestHelper.getExpectedLiteEngineTaskInfoOnFirstPod(), null, true);

    Map<String, EncryptedVariableWithType> envSecretVars = new HashMap<>();
    envSecretVars.put(ACCESS_KEY_MINIO_VARIABLE, null);
    envSecretVars.put(SECRET_KEY_MINIO_VARIABLE, null);
    envSecretVars.putAll(ciExecutionPlanTestHelper.getEncryptedSecrets());

    Map<String, String> stepEnvVars = new HashMap<>();
    stepEnvVars.put(LOG_SERVICE_ENDPOINT_VARIABLE, LOG_SERVICE_ENDPOINT_VARIABLE_VALUE);
    stepEnvVars.put(HARNESS_ACCOUNT_ID_VARIABLE, accountID);
    stepEnvVars.put(HARNESS_ORG_ID_VARIABLE, orgID);
    stepEnvVars.put(HARNESS_PROJECT_ID_VARIABLE, projectID);
    stepEnvVars.put(HARNESS_BUILD_ID_VARIABLE, buildID.toString());
    stepEnvVars.put(HARNESS_STAGE_ID_VARIABLE, stageID);
    stepEnvVars.putAll(ciExecutionPlanTestHelper.getEnvVars());

    Map<String, String> map = new HashMap<>();
    map.put(STEP_EXEC, MOUNT_PATH);
    String workDir = String.format("/%s/%s", STEP_EXEC, STEP_EXEC_WORKING_DIR);
    assertThat(podParams.getContainerParamsList().get(0))
        .isEqualToIgnoringGivenFields(
            ciExecutionPlanTestHelper.getRunStepCIK8Container().volumeToMountPath(map).workingDir(workDir).build(),
            "envVars", "containerSecrets");
    assertThat(podParams.getContainerParamsList().get(0).getContainerSecrets().getEncryptedSecrets())
        .containsAllEntriesOf(envSecretVars);
    assertThat(podParams.getContainerParamsList().get(0).getEnvVars()).containsAllEntriesOf(stepEnvVars);

    assertThat(podParams.getContainerParamsList().get(1))
        .isEqualToIgnoringGivenFields(
            ciExecutionPlanTestHelper.getPluginStepCIK8Container().volumeToMountPath(map).workingDir(workDir).build(),
            "envVars", "containerSecrets");

    assertThat(podParams.getInitContainerParamsList().get(0))
        .isEqualTo(InternalContainerParamsProvider.getSetupAddonContainerParams()
                       .containerSecrets(ContainerSecrets.builder().build())
                       .build());
  }
}
