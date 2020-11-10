package io.harness.stateutils.buildstate;

import static io.harness.common.CIExecutionConstants.ACCESS_KEY_MINIO_VARIABLE;
import static io.harness.common.CIExecutionConstants.DELEGATE_SERVICE_TOKEN_VARIABLE;
import static io.harness.common.CIExecutionConstants.HARNESS_ACCOUNT_ID_VARIABLE;
import static io.harness.common.CIExecutionConstants.HARNESS_BUILD_ID_VARIABLE;
import static io.harness.common.CIExecutionConstants.HARNESS_ORG_ID_VARIABLE;
import static io.harness.common.CIExecutionConstants.HARNESS_PROJECT_ID_VARIABLE;
import static io.harness.common.CIExecutionConstants.HARNESS_STAGE_ID_VARIABLE;
import static io.harness.common.CIExecutionConstants.LOG_SERVICE_ENDPOINT_VARIABLE;
import static io.harness.common.CIExecutionConstants.LOG_SERVICE_TOKEN_VARIABLE;
import static io.harness.common.CIExecutionConstants.SECRET_KEY_MINIO_VARIABLE;
import static io.harness.rule.OwnerRule.HARSH;
import static io.harness.rule.OwnerRule.VISTAAR;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.joor.Reflect.on;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.common.CICommonPodConstants.MOUNT_PATH;
import static software.wings.common.CICommonPodConstants.STEP_EXEC;
import static software.wings.common.CICommonPodConstants.STEP_EXEC_WORKING_DIR;

import com.google.inject.Inject;

import io.harness.beans.environment.pod.PodSetupInfo;
import io.harness.beans.sweepingoutputs.K8PodDetails;
import io.harness.beans.sweepingoutputs.StepTaskDetails;
import io.harness.category.element.UnitTests;
import io.harness.ci.beans.entities.BuildNumberDetails;
import io.harness.ci.beans.entities.LogServiceConfig;
import io.harness.encryption.Scope;
import io.harness.encryption.SecretRefData;
import io.harness.engine.outputs.ExecutionSweepingOutputService;
import io.harness.exception.GeneralException;
import io.harness.executionplan.CIExecutionPlanTestHelper;
import io.harness.executionplan.CIExecutionTest;
import io.harness.logserviceclient.CILogServiceUtils;
import io.harness.ng.core.BaseNGAccess;
import io.harness.ng.core.NGAccess;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ng.core.dto.secrets.SecretDTOV2;
import io.harness.ng.core.dto.secrets.SecretResponseWrapper;
import io.harness.rule.Owner;
import io.harness.secretmanagerclient.SecretType;
import io.harness.secretmanagerclient.services.api.SecretManagerClientService;
import io.harness.secrets.remote.SecretNGManagerClient;
import io.harness.security.encryption.EncryptedDataDetail;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import retrofit2.Call;
import retrofit2.Response;
import software.wings.beans.ci.pod.CIK8ContainerParams;
import software.wings.beans.ci.pod.CIK8PodParams;
import software.wings.beans.ci.pod.ConnectorDetails;
import software.wings.beans.ci.pod.SecretVariableDTO;
import software.wings.beans.ci.pod.SecretVariableDetails;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class K8BuildSetupUtilsTest extends CIExecutionTest {
  @Inject private CIExecutionPlanTestHelper ciExecutionPlanTestHelper;
  @Inject private K8BuildSetupUtils k8BuildSetupUtils;
  @Inject private SecretVariableUtils secretVariableUtils;

  @Mock private ExecutionSweepingOutputService executionSweepingOutputResolver;
  @Mock private SecretManagerClientService secretManagerClientService;
  @Mock private SecretNGManagerClient secretNGManagerClient;
  @Mock private ConnectorUtils connectorUtils;
  @Mock CILogServiceUtils logServiceUtils;

  @Before
  public void setUp() {
    on(k8BuildSetupUtils).set("connectorUtils", connectorUtils);
    on(secretVariableUtils).set("secretNGManagerClient", secretNGManagerClient);
    on(secretVariableUtils).set("secretManagerClientService", secretManagerClientService);
    on(k8BuildSetupUtils).set("secretVariableUtils", secretVariableUtils);
    on(k8BuildSetupUtils).set("executionSweepingOutputResolver", executionSweepingOutputResolver);
    on(k8BuildSetupUtils).set("logServiceUtils", logServiceUtils);
  }

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  public void shouldCreatePodParameters() throws Exception {
    String accountID = "account";
    String orgID = "org";
    String projectID = "project";
    Long buildID = 1L;
    String stageID = "stage";
    String namespace = "default";

    String logEndpoint = "http://localhost:8080";
    String logToken = "token";
    LogServiceConfig logServiceConfig = LogServiceConfig.builder().baseUrl(logEndpoint).globalToken(logToken).build();
    when(logServiceUtils.getLogServiceConfig()).thenReturn(logServiceConfig);
    when(logServiceUtils.getLogServiceToken(eq(accountID))).thenReturn(logToken);

    Call<ResponseDTO<SecretResponseWrapper>> getSecretCall = mock(Call.class);
    ResponseDTO<SecretResponseWrapper> responseDTO = ResponseDTO.newResponse(
        SecretResponseWrapper.builder().secret(SecretDTOV2.builder().type(SecretType.SecretText).build()).build());
    when(getSecretCall.execute()).thenReturn(Response.success(responseDTO));
    when(secretNGManagerClient.getSecret(any(), any(), any(), any())).thenReturn(getSecretCall);
    when(secretManagerClientService.getEncryptionDetails(any(), any()))
        .thenReturn(Collections.singletonList(EncryptedDataDetail.builder().build()));
    when(executionSweepingOutputResolver.resolve(any(), any())).thenReturn(StepTaskDetails.builder().build());
    when(connectorUtils.getConnectorDetails(any(), any())).thenReturn(ConnectorDetails.builder().build());

    PodSetupInfo podsSetupInfo = ciExecutionPlanTestHelper.getCIPodsSetupInfoOnFirstPod().getPodSetupInfoList().get(0);

    BuildNumberDetails buildNumberDetails = BuildNumberDetails.builder()
                                                .accountIdentifier(accountID)
                                                .orgIdentifier(orgID)
                                                .projectIdentifier(projectID)
                                                .buildNumber(buildID)
                                                .build();
    NGAccess ngAccess =
        BaseNGAccess.builder().accountIdentifier(accountID).orgIdentifier(orgID).projectIdentifier(projectID).build();
    K8PodDetails k8PodDetails =
        K8PodDetails.builder().namespace(namespace).buildNumberDetails(buildNumberDetails).stageID(stageID).build();

    CIK8PodParams<CIK8ContainerParams> podParams = k8BuildSetupUtils.getPodParams(ngAccess, podsSetupInfo, k8PodDetails,
        ciExecutionPlanTestHelper.getExpectedLiteEngineTaskInfoOnFirstPodWithSetCallbackId(), null, true, null, true);

    List<SecretVariableDetails> secretVariableDetails =
        new ArrayList<>(ciExecutionPlanTestHelper.getSecretVariableDetails());
    secretVariableDetails.add(
        SecretVariableDetails.builder()
            .secretVariableDTO(
                SecretVariableDTO.builder()
                    .type(SecretVariableDTO.Type.TEXT)
                    .name(ACCESS_KEY_MINIO_VARIABLE)
                    .secret(SecretRefData.builder().identifier(ACCESS_KEY_MINIO_VARIABLE).scope(Scope.ACCOUNT).build())
                    .build())
            .encryptedDataDetailList(singletonList(EncryptedDataDetail.builder().build()))
            .build());
    secretVariableDetails.add(
        SecretVariableDetails.builder()
            .secretVariableDTO(
                SecretVariableDTO.builder()
                    .type(SecretVariableDTO.Type.TEXT)
                    .name(SECRET_KEY_MINIO_VARIABLE)
                    .secret(SecretRefData.builder().identifier(SECRET_KEY_MINIO_VARIABLE).scope(Scope.ACCOUNT).build())
                    .build())
            .encryptedDataDetailList(singletonList(EncryptedDataDetail.builder().build()))
            .build());

    Map<String, String> stepEnvVars = new HashMap<>();
    stepEnvVars.put(LOG_SERVICE_ENDPOINT_VARIABLE, logEndpoint);
    stepEnvVars.put(LOG_SERVICE_TOKEN_VARIABLE, logToken);
    stepEnvVars.put(HARNESS_ACCOUNT_ID_VARIABLE, accountID);
    stepEnvVars.put(HARNESS_ORG_ID_VARIABLE, orgID);
    stepEnvVars.put(HARNESS_PROJECT_ID_VARIABLE, projectID);
    stepEnvVars.put(HARNESS_BUILD_ID_VARIABLE, buildID.toString());
    stepEnvVars.put(HARNESS_STAGE_ID_VARIABLE, stageID);
    stepEnvVars.putAll(ciExecutionPlanTestHelper.getEnvVariables());

    Map<String, String> map = new HashMap<>();
    map.put(STEP_EXEC, MOUNT_PATH);
    String workDir = String.format("/%s/%s", STEP_EXEC, STEP_EXEC_WORKING_DIR);
    assertThat(podParams.getContainerParamsList().get(2))
        .isEqualToIgnoringGivenFields(
            ciExecutionPlanTestHelper.getRunStepCIK8Container().volumeToMountPath(map).workingDir(workDir).build(),
            "envVars", "containerSecrets");
    assertThat(podParams.getContainerParamsList().get(2).getContainerSecrets().getSecretVariableDetails())
        .containsAnyElementsOf(secretVariableDetails);
    assertThat(podParams.getContainerParamsList().get(2).getEnvVars()).containsAllEntriesOf(stepEnvVars);

    stepEnvVars.put(DELEGATE_SERVICE_TOKEN_VARIABLE,
        podParams.getContainerParamsList().get(3).getEnvVars().get(DELEGATE_SERVICE_TOKEN_VARIABLE));
    assertThat(podParams.getContainerParamsList().get(3))
        .isEqualToIgnoringGivenFields(
            ciExecutionPlanTestHelper.getPluginStepCIK8Container().build(), "envVars", "containerSecrets");

    assertThat(podParams.getContainerParamsList().get(1))
        .isEqualToIgnoringGivenFields(
            ciExecutionPlanTestHelper.getGitCloneStepCIK8Container().build(), "envVars", "containerSecrets");

    assertThat(podParams.getContainerParamsList().get(0))
        .isEqualToIgnoringGivenFields(
            ciExecutionPlanTestHelper.getServiceCIK8Container(), "envVars", "containerSecrets");

    verify(logServiceUtils, times(1)).getLogServiceConfig();
    verify(logServiceUtils, times(1)).getLogServiceToken(accountID);
  }

  @Test
  @Owner(developers = VISTAAR)
  @Category(UnitTests.class)
  public void shouldNotCreatePodParameters() throws Exception {
    String accountID = "account";
    String orgID = "org";
    String projectID = "project";
    Long buildID = 1L;
    String stageID = "stage";
    String namespace = "default";

    String logEndpoint = "http://localhost:8080";
    String logToken = "token";
    LogServiceConfig logServiceConfig = LogServiceConfig.builder().baseUrl(logEndpoint).globalToken(logToken).build();
    when(logServiceUtils.getLogServiceConfig()).thenReturn(logServiceConfig);
    when(logServiceUtils.getLogServiceToken(eq(accountID))).thenThrow(new GeneralException("Could not retrieve token"));

    PodSetupInfo podsSetupInfo = ciExecutionPlanTestHelper.getCIPodsSetupInfoOnFirstPod().getPodSetupInfoList().get(0);

    BuildNumberDetails buildNumberDetails = BuildNumberDetails.builder()
                                                .accountIdentifier(accountID)
                                                .orgIdentifier(orgID)
                                                .projectIdentifier(projectID)
                                                .buildNumber(buildID)
                                                .build();
    NGAccess ngAccess =
        BaseNGAccess.builder().accountIdentifier(accountID).orgIdentifier(orgID).projectIdentifier(projectID).build();
    K8PodDetails k8PodDetails =
        K8PodDetails.builder().namespace(namespace).buildNumberDetails(buildNumberDetails).stageID(stageID).build();

    assertThatThrownBy(
        ()
            -> k8BuildSetupUtils.getPodParams(ngAccess, podsSetupInfo, k8PodDetails,
                ciExecutionPlanTestHelper.getExpectedLiteEngineTaskInfoOnFirstPod(), null, true, null, true))
        .isInstanceOf(Exception.class);

    verify(logServiceUtils, times(1)).getLogServiceConfig();
    verify(logServiceUtils, times(1)).getLogServiceToken(accountID);
  }
}
