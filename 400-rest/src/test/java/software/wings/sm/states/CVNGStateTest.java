package software.wings.sm.states;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.KAMAL;

import static software.wings.sm.states.CVNGState.DEFAULT_HOSTNAME_TEMPLATE;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.beans.ExecutionStatus;
import io.harness.category.element.UnitTests;
import io.harness.cvng.beans.activity.ActivityDTO;
import io.harness.cvng.beans.activity.ActivityStatusDTO;
import io.harness.cvng.beans.activity.ActivityVerificationStatus;
import io.harness.cvng.beans.activity.DeploymentActivityDTO;
import io.harness.cvng.beans.activity.cd10.CD10RegisterActivityDTO;
import io.harness.cvng.client.CVNGService;
import io.harness.cvng.state.CVNGVerificationTask;
import io.harness.cvng.state.CVNGVerificationTaskService;
import io.harness.rule.Owner;
import io.harness.tasks.ResponseData;

import software.wings.api.ExecutionDataValue;
import software.wings.api.instancedetails.InstanceApiResponse;
import software.wings.beans.WorkflowExecution;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.StateExecutionData;
import software.wings.sm.states.CVNGState.CVNGStateExecutionData;
import software.wings.sm.states.CVNGState.CVNGStateExecutionData.CVNGStateExecutionDataKeys;
import software.wings.sm.states.CVNGState.CVNGStateResponseData;
import software.wings.sm.states.CVNGState.ParamValue;

import com.google.common.collect.Lists;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.internal.util.collections.Sets;

public class CVNGStateTest extends CategoryTest {
  @Mock private CVNGVerificationTaskService cvngVerificationTaskService;
  @Mock private CVNGService cvngService;
  @Mock private WorkflowExecutionService workflowExecutionService;
  @InjectMocks private CVNGState cvngState = new CVNGState("cvngState");
  @Mock ExecutionContext executionContext;
  private Instant now;
  private Instant workflowStartTime;
  private Clock clock;
  private String serviceId;
  private String projectIdentifier;
  private String orgIdentifier;
  private String deploymentTag;
  private String verificationJobIdentifier;
  private String envId;

  @Before
  public void setup() throws IllegalAccessException {
    MockitoAnnotations.initMocks(this);
    clock = Clock.fixed(Instant.parse("2020-04-22T10:02:06Z"), ZoneOffset.UTC);
    now = clock.instant();
    serviceId = generateUuid();
    envId = generateUuid();
    workflowStartTime = now.minus(Duration.ofMinutes(15));
    when(executionContext.getAppId()).thenReturn(APP_ID);
    when(executionContext.getAccountId()).thenReturn(ACCOUNT_ID);
    when(executionContext.getWorkflowExecutionName()).thenReturn("Workflow Execution name");
    when(executionContext.renderExpressionsForInstanceDetailsForWorkflow(eq(DEFAULT_HOSTNAME_TEMPLATE), eq(false)))
        .thenReturn(InstanceApiResponse.builder().instances(Lists.newArrayList("h1", "h2", "h3")).build());
    when(executionContext.renderExpressionsForInstanceDetailsForWorkflow(eq(DEFAULT_HOSTNAME_TEMPLATE), eq(true)))
        .thenReturn(InstanceApiResponse.builder().instances(Lists.newArrayList("h1", "h2")).build());
    when(executionContext.renderExpressionsForInstanceDetails(eq(DEFAULT_HOSTNAME_TEMPLATE), eq(true)))
        .thenReturn(InstanceApiResponse.builder().instances(Lists.newArrayList("h2")).build());
    when(executionContext.renderExpression(any())).then(v -> {
      String value = (String) v.getArguments()[0];
      if (value.equals("${expression}")) {
        return "renderedExpression";
      }
      return value;
    });
    when(workflowExecutionService.getWorkflowExecution(any(), any()))
        .thenReturn(WorkflowExecution.builder()
                        .startTs(workflowStartTime.toEpochMilli())
                        .serviceIds(Collections.singletonList(serviceId))
                        .envId(envId)
                        .build());
    FieldUtils.writeField(cvngState, "clock", clock, true);
    projectIdentifier = generateUuid();
    orgIdentifier = generateUuid();
    deploymentTag = generateUuid();
    verificationJobIdentifier = generateUuid();
    cvngState.setProjectIdentifier(projectIdentifier);
    cvngState.setOrgIdentifier(orgIdentifier);
    cvngState.setDeploymentTag(deploymentTag);
    cvngState.setVerificationJobIdentifier(verificationJobIdentifier);
    cvngState.setCvngParams(Lists.newArrayList(ParamValue.builder().name("serviceIdentifier").value("service").build(),
        ParamValue.builder().name("envIdentifier").value("env").build(),
        ParamValue.builder().name("duration").value("20m").editable(true).build()));
    cvngState.setWebhookUrl(
        "https://localhost:6060/cv/api/verification-job?accountId=zEaak-FLS425IEO7OLzMUg&orgIdentifier=CV_Stable&projectIdentifier=cv_demo&identifier=canary");
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testExecute_failWithExceptionMessage() {
    String errorMsg = "error message when calling nextgen service";
    when(cvngService.registerActivity(anyString(), any())).thenThrow(new IllegalStateException(errorMsg));
    ExecutionResponse executionResponse = cvngState.execute(executionContext);
    assertThat(executionResponse.getExecutionStatus()).isEqualTo(ExecutionStatus.FAILED);
    assertThat(executionResponse.getErrorMessage()).isEqualTo(errorMsg);
    assertThat(executionResponse.isAsync()).isFalse();
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testExecute_createActivityWithCVNGVerificationTask() {
    String activityId = generateUuid();
    when(cvngService.registerActivity(anyString(), any()))
        .thenReturn(CD10RegisterActivityDTO.builder()
                        .activityId(activityId)
                        .serviceIdentifier("service")
                        .envIdentifier("env")
                        .build());
    ExecutionResponse executionResponse = cvngState.execute(executionContext);
    ArgumentCaptor<CVNGVerificationTask> cvngVerificationTaskArgumentCaptor =
        ArgumentCaptor.forClass(CVNGVerificationTask.class);
    verify(cvngVerificationTaskService, times(1)).create(cvngVerificationTaskArgumentCaptor.capture());
    assertThat(executionResponse.getExecutionStatus()).isEqualTo(ExecutionStatus.RUNNING);
    assertThat(executionResponse.getErrorMessage()).isEqualTo(null);
    assertThat(executionResponse.isAsync()).isTrue();
    assertThat(executionResponse.getStateExecutionData())
        .isEqualTo(CVNGStateExecutionData.builder()
                       .activityId(activityId)
                       .projectIdentifier(projectIdentifier)
                       .orgIdentifier(orgIdentifier)
                       .deploymentTag(deploymentTag)
                       .serviceIdentifier("service")
                       .envIdentifier("env")
                       .build());
    CVNGVerificationTask verificationTask = cvngVerificationTaskArgumentCaptor.getValue();
    assertThat(verificationTask)
        .isEqualTo(CVNGVerificationTask.builder()
                       .accountId(ACCOUNT_ID)
                       .activityId(activityId)
                       .startTime(now)
                       .status(CVNGVerificationTask.Status.IN_PROGRESS)
                       .correlationId(executionResponse.getCorrelationIds().get(0))
                       .build());
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testExecute_verifyDeploymentActivityDTOValues() {
    String activityId = generateUuid();
    when(cvngService.registerActivity(anyString(), any()))
        .thenReturn(CD10RegisterActivityDTO.builder()
                        .activityId(activityId)
                        .serviceIdentifier("service")
                        .envIdentifier("env")
                        .build());
    ExecutionResponse executionResponse = cvngState.execute(executionContext);
    ArgumentCaptor<DeploymentActivityDTO> captor = ArgumentCaptor.forClass(DeploymentActivityDTO.class);
    verify(cvngService, times(1)).registerActivity(eq(ACCOUNT_ID), captor.capture());
    assertThat(executionResponse.getExecutionStatus()).isEqualTo(ExecutionStatus.RUNNING);
    DeploymentActivityDTO deploymentActivityDTO = captor.getValue();
    assertThat(deploymentActivityDTO.getAccountIdentifier()).isEqualTo(ACCOUNT_ID);
    assertThat(deploymentActivityDTO.getDeploymentTag()).isEqualTo(deploymentTag);
    assertThat(deploymentActivityDTO.getDataCollectionDelayMs()).isNull();
    assertThat(deploymentActivityDTO.getVerificationStartTime()).isEqualTo(now.toEpochMilli());
    assertThat(deploymentActivityDTO.getActivityStartTime()).isEqualTo(workflowStartTime.toEpochMilli());
    assertThat(deploymentActivityDTO.getNewVersionHosts()).isEqualTo(Sets.newSet("h2"));
    assertThat(deploymentActivityDTO.getOldVersionHosts()).isEqualTo(Sets.newSet("h3"));
    assertThat(deploymentActivityDTO.getProjectIdentifier()).isEqualTo(projectIdentifier);
    assertThat(deploymentActivityDTO.getOrgIdentifier()).isEqualTo(orgIdentifier);
    assertThat(deploymentActivityDTO.getServiceIdentifier()).isNull();
    assertThat(deploymentActivityDTO.getEnvironmentIdentifier()).isNull();
    Map<String, String> runtimeValuesExpectations = new HashMap<>();
    runtimeValuesExpectations.put("harnessCdAppId", APP_ID);
    runtimeValuesExpectations.put("harnessCdServiceId", serviceId);
    runtimeValuesExpectations.put("harnessCdEnvId", envId);
    runtimeValuesExpectations.put("duration", "20m");
    assertThat(deploymentActivityDTO.getVerificationJobRuntimeDetails())
        .isEqualTo(Collections.singletonList(ActivityDTO.VerificationJobRuntimeDetails.builder()
                                                 .verificationJobIdentifier(verificationJobIdentifier)
                                                 .runtimeValues(runtimeValuesExpectations)
                                                 .build()));
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testExecute_validateServiceAndEnvIdentifierIfRuntimeParams() {
    String activityId = generateUuid();
    when(cvngService.registerActivity(anyString(), any()))
        .thenReturn(CD10RegisterActivityDTO.builder()
                        .activityId(activityId)
                        .serviceIdentifier("service")
                        .envIdentifier("env")
                        .build());
    cvngState.setCvngParams(
        Lists.newArrayList(ParamValue.builder().name("serviceIdentifier").value("<+input>").editable(false).build(),
            ParamValue.builder().name("envIdentifier").editable(false).value("<+input>").build()));
    ExecutionResponse executionResponse = cvngState.execute(executionContext);
    ArgumentCaptor<DeploymentActivityDTO> captor = ArgumentCaptor.forClass(DeploymentActivityDTO.class);
    verify(cvngService, times(1)).registerActivity(eq(ACCOUNT_ID), captor.capture());
    assertThat(executionResponse.getExecutionStatus()).isEqualTo(ExecutionStatus.RUNNING);
    DeploymentActivityDTO deploymentActivityDTO = captor.getValue();
    assertThat(deploymentActivityDTO.getServiceIdentifier()).isNull();
    assertThat(deploymentActivityDTO.getEnvironmentIdentifier()).isNull();
    Map<String, String> runtimeValuesExpectations = new HashMap<>();
    runtimeValuesExpectations.put("harnessCdAppId", APP_ID);
    runtimeValuesExpectations.put("harnessCdServiceId", serviceId);
    runtimeValuesExpectations.put("harnessCdEnvId", envId);
    assertThat(deploymentActivityDTO.getVerificationJobRuntimeDetails())
        .isEqualTo(Collections.singletonList(ActivityDTO.VerificationJobRuntimeDetails.builder()
                                                 .verificationJobIdentifier(verificationJobIdentifier)
                                                 .runtimeValues(runtimeValuesExpectations)
                                                 .build()));
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testExecute_testRenderExpression() {
    String activityId = generateUuid();
    when(cvngService.registerActivity(anyString(), any()))
        .thenReturn(CD10RegisterActivityDTO.builder()
                        .activityId(activityId)
                        .serviceIdentifier("service")
                        .envIdentifier("env")
                        .build());
    cvngState.setDeploymentTag("${expression}");
    cvngState.setCvngParams(
        Lists.newArrayList(ParamValue.builder().name("serviceIdentifier").value("<+input>").editable(false).build(),
            ParamValue.builder().name("envIdentifier").value("<+input>").editable(false).build(),
            ParamValue.builder().name("runtimeField").value("${expression}").editable(true).build()));
    ExecutionResponse executionResponse = cvngState.execute(executionContext);
    ArgumentCaptor<DeploymentActivityDTO> captor = ArgumentCaptor.forClass(DeploymentActivityDTO.class);
    verify(cvngService, times(1)).registerActivity(eq(ACCOUNT_ID), captor.capture());
    assertThat(executionResponse.getExecutionStatus()).isEqualTo(ExecutionStatus.RUNNING);
    assertThat(executionResponse.getStateExecutionData())
        .isEqualTo(CVNGStateExecutionData.builder()
                       .activityId(activityId)
                       .projectIdentifier(projectIdentifier)
                       .orgIdentifier(orgIdentifier)
                       .deploymentTag("renderedExpression")
                       .serviceIdentifier("service")
                       .envIdentifier("env")
                       .build());
    DeploymentActivityDTO deploymentActivityDTO = captor.getValue();
    assertThat(deploymentActivityDTO.getServiceIdentifier()).isNull();
    assertThat(deploymentActivityDTO.getEnvironmentIdentifier()).isNull();
    assertThat(deploymentActivityDTO.getDeploymentTag()).isEqualTo("renderedExpression");
    Map<String, String> runtimeValuesExpectations = new HashMap<>();
    runtimeValuesExpectations.put("harnessCdAppId", APP_ID);
    runtimeValuesExpectations.put("harnessCdServiceId", serviceId);
    runtimeValuesExpectations.put("harnessCdEnvId", envId);
    runtimeValuesExpectations.put("runtimeField", "renderedExpression");
    assertThat(deploymentActivityDTO.getVerificationJobRuntimeDetails())
        .isEqualTo(Collections.singletonList(ActivityDTO.VerificationJobRuntimeDetails.builder()
                                                 .verificationJobIdentifier(verificationJobIdentifier)
                                                 .runtimeValues(runtimeValuesExpectations)
                                                 .build()));
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testExecute_parseDataCollectionDelayIfNull() {
    String activityId = generateUuid();
    when(cvngService.registerActivity(anyString(), any()))
        .thenReturn(CD10RegisterActivityDTO.builder()
                        .activityId(activityId)
                        .serviceIdentifier("service")
                        .envIdentifier("env")
                        .build());
    cvngState.execute(executionContext);
    ArgumentCaptor<DeploymentActivityDTO> captor = ArgumentCaptor.forClass(DeploymentActivityDTO.class);
    verify(cvngService, times(1)).registerActivity(eq(ACCOUNT_ID), captor.capture());
    DeploymentActivityDTO deploymentActivityDTO = captor.getValue();
    assertThat(deploymentActivityDTO.getDataCollectionDelayMs()).isNull();
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testExecute_parseDataCollectionDelay() {
    String activityId = generateUuid();
    when(cvngService.registerActivity(anyString(), any()))
        .thenReturn(CD10RegisterActivityDTO.builder()
                        .activityId(activityId)
                        .serviceIdentifier("service")
                        .envIdentifier("env")
                        .build());
    cvngState.setDataCollectionDelay("11m");
    cvngState.execute(executionContext);
    ArgumentCaptor<DeploymentActivityDTO> captor = ArgumentCaptor.forClass(DeploymentActivityDTO.class);
    verify(cvngService, times(1)).registerActivity(eq(ACCOUNT_ID), captor.capture());
    DeploymentActivityDTO deploymentActivityDTO = captor.getValue();
    assertThat(deploymentActivityDTO.getDataCollectionDelayMs()).isEqualTo(Duration.ofMinutes(11).toMillis());
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testHandleAsyncResponse_verificationSuccess() {
    Map<String, ResponseData> responseMap = new HashMap<>();
    String activityId = generateUuid();
    StateExecutionData stateExecutionData = CVNGStateExecutionData.builder()
                                                .activityId(activityId)
                                                .projectIdentifier(projectIdentifier)
                                                .orgIdentifier(orgIdentifier)
                                                .deploymentTag(deploymentTag)
                                                .serviceIdentifier("service")
                                                .envIdentifier("env")
                                                .build();
    when(executionContext.getStateExecutionData()).thenReturn(stateExecutionData);
    ActivityStatusDTO activityStatusDTO =
        ActivityStatusDTO.builder().status(ActivityVerificationStatus.VERIFICATION_PASSED).build();
    CVNGStateResponseData cvngStateResponseData = CVNGStateResponseData.builder()
                                                      .correlationId(generateUuid())
                                                      .activityId(activityId)
                                                      .status(CVNGVerificationTask.Status.DONE)
                                                      .activityStatusDTO(activityStatusDTO)
                                                      .build();
    responseMap.put(generateUuid(), cvngStateResponseData);
    ExecutionResponse executionResponse = cvngState.handleAsyncResponse(executionContext, responseMap);
    assertThat(executionResponse.isAsync()).isFalse();
    assertThat(executionResponse.getExecutionStatus()).isEqualTo(ExecutionStatus.SUCCESS);
    assertThat(executionResponse.getStateExecutionData()).isEqualTo(stateExecutionData);
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testHandleAsyncResponse_executionStatus() {
    Map<String, ResponseData> responseMap = new HashMap<>();
    String activityId = generateUuid();
    StateExecutionData stateExecutionData = CVNGStateExecutionData.builder()
                                                .activityId(activityId)
                                                .projectIdentifier(projectIdentifier)
                                                .orgIdentifier(orgIdentifier)
                                                .deploymentTag(deploymentTag)
                                                .serviceIdentifier("service")
                                                .envIdentifier("env")
                                                .build();
    when(executionContext.getStateExecutionData()).thenReturn(stateExecutionData);
    ActivityStatusDTO activityStatusDTO =
        ActivityStatusDTO.builder().status(ActivityVerificationStatus.VERIFICATION_PASSED).build();
    CVNGStateResponseData cvngStateResponseData = CVNGStateResponseData.builder()
                                                      .correlationId(generateUuid())
                                                      .executionStatus(ExecutionStatus.EXPIRED)
                                                      .activityId(activityId)
                                                      .status(CVNGVerificationTask.Status.DONE)
                                                      .activityStatusDTO(activityStatusDTO)
                                                      .build();
    responseMap.put(generateUuid(), cvngStateResponseData);
    ExecutionResponse executionResponse = cvngState.handleAsyncResponse(executionContext, responseMap);
    assertThat(executionResponse.isAsync()).isFalse();
    assertThat(executionResponse.getExecutionStatus()).isEqualTo(ExecutionStatus.EXPIRED);
    assertThat(executionResponse.getStateExecutionData()).isEqualTo(stateExecutionData);
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testGetExecutionDetails_forCVNGStateExecutionData() {
    String activityId = generateUuid();
    StateExecutionData stateExecutionData = CVNGStateExecutionData.builder()
                                                .activityId(activityId)
                                                .projectIdentifier(projectIdentifier)
                                                .orgIdentifier(orgIdentifier)
                                                .deploymentTag(deploymentTag)
                                                .serviceIdentifier("service")
                                                .envIdentifier("env")
                                                .build();
    stateExecutionData.setStatus(ExecutionStatus.RUNNING);
    Map<String, String> idsMap = new HashMap<>();
    idsMap.put(CVNGStateExecutionDataKeys.activityId, activityId);
    idsMap.put(CVNGStateExecutionDataKeys.orgIdentifier, orgIdentifier);
    idsMap.put(CVNGStateExecutionDataKeys.projectIdentifier, projectIdentifier);
    idsMap.put(CVNGStateExecutionDataKeys.envIdentifier, "env");
    idsMap.put(CVNGStateExecutionDataKeys.serviceIdentifier, "service");
    idsMap.put(CVNGStateExecutionDataKeys.deploymentTag, deploymentTag);
    assertThat(stateExecutionData.getExecutionDetails().get("cvngIds"))
        .isEqualTo(ExecutionDataValue.builder().displayName("cvngIds").value(idsMap).build());
    assertThat(stateExecutionData.getExecutionSummary().get("cvngIds"))
        .isEqualTo(ExecutionDataValue.builder().displayName("cvngIds").value(idsMap).build());
  }
}